/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.										*
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net or http://www.adempiere.net/license.html         *
 *****************************************************************************/

package org.spin.process;

import java.util.Hashtable;

import org.compiere.model.*;
import org.compiere.util.Env;
import org.compiere.util.Msg;

/** Generated Process for (Create Purchase Order)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.1
 */
public class CommissionOrderCreate extends CommissionOrderCreateAbstract {
	/**	Invoices created	*/
	private int created = 0;
	/**	Message	*/
	private StringBuffer generatedDocuments = new StringBuffer();
	/**	Invoices	*/
	private Hashtable<Integer, MOrder> orders;
	
	/**
	 *  Perform process.
	 *  @return Message (variables are parsed)
	 *  @throws Exception if not successful
	 */
	protected String doIt() throws Exception {
		log.info("doIt - C_CommissionRun_ID=" + getRecord_ID());
		//	Load Data
		MCommissionRun commissionRun = new MCommissionRun (getCtx(), getRecord_ID(), get_TrxName());
		if (commissionRun.getC_CommissionRun_ID() == 0)
			throw new IllegalArgumentException("@C_CommissionRun_ID@ @NotFound@");
		if (Env.ZERO.compareTo(commissionRun.getGrandTotal()) == 0)
			throw new IllegalArgumentException("@GrandTotal@ = 0");
		//	For Completed Commission
		if(!commissionRun.getDocStatus().equals(MCommissionRun.ACTION_Complete)) {
			throw new IllegalArgumentException("@C_CommissionRun_ID@ @NotValid@");
		}
		//	
		MCommission commissionDefinition = new MCommission (getCtx(), commissionRun.getC_Commission_ID(), get_TrxName());
		if (commissionDefinition.getC_Commission_ID() == 0)
			throw new IllegalArgumentException("@C_Commission_ID@ @NotFound@");
		if (commissionDefinition.getC_Charge_ID() == 0
				&& commissionDefinition.get_ValueAsInt("M_Product_ID") == 0)
			throw new IllegalArgumentException("@C_Commission_ID@ - (@M_Product_ID@ / @C_Charge_ID@) @NotFound@");
		//	
		orders = new Hashtable<>();
		//	Get lines
		commissionRun.getCommissionAmtList().stream()
			.filter(commissionAmt -> commissionAmt.getCommissionAmt() != null 
				&& commissionAmt.getCommissionAmt().compareTo(Env.ZERO) > 0).forEach(commissionAmt -> {
			MOrder order = getOrder(commissionDefinition, commissionRun, commissionAmt);
			createOrderLine(commissionAmt, order, commissionDefinition.getC_Charge_ID(), commissionDefinition.get_ValueAsInt("M_Product_ID"));
		});
		//	Process orders
		if(orders.size() > 0) {
			orders.entrySet().stream().forEach(orderSet ->{
				MOrder order = orderSet.getValue();
				order.processIt(getDocAction());
				order.saveEx();
			});
		}
		//
		return "@Created@ " + created + (generatedDocuments.length() > 0? " [" + generatedDocuments + "]": "");
	}	//	doIt
	
	/**
	 * Create Invoice header
	 */
	private MOrder getOrder(MCommission commissionDefinition, MCommissionRun commissionRun, MCommissionAmt commissionAmt) {
		int bPartnerId = getBPartnerId();
		if(bPartnerId <= 0) {
			bPartnerId = commissionAmt.getC_BPartner_ID();
		}
		MOrder order = orders.get(bPartnerId);
		if(order != null) {
			return order;
		}
		//	Create Invoice
		order = new MOrder (getCtx(), 0, get_TrxName());
		order.setClientOrg(commissionAmt.getAD_Client_ID(), commissionAmt.getAD_Org_ID());
		order.setIsSOTrx(isSOTrx());
		if(getDocTypeId() > 0) {
			order.setC_DocTypeTarget_ID(getDocTypeId());
		} else {
			order.setC_DocTypeTarget_ID();	//	POO
		}
		MBPartner businessPartner = MBPartner.get(getCtx(), bPartnerId);
		order.setBPartner(businessPartner);


		// Openup Solutions - #13739 - Raul Capecce - 09/04/2020
		// Definición del SDN BillTo en base a relación en C_BP_Relation
		String billBPartnerWhere = I_C_BP_Relation.COLUMNNAME_C_BPartner_ID + "=? AND " + I_C_BP_Relation.COLUMNNAME_IsBillTo + "='Y'";
		PO bpRelation = new Query(order.getCtx(), I_C_BP_Relation.Table_Name, billBPartnerWhere, order.get_TrxName())
				.setParameters(order.getC_BPartner_ID())
				.setOnlyActiveRecords(true)
				.first();
		MBPartner billBPartner = null;
		if (bpRelation != null) {
			billBPartner = new MBPartner(order.getCtx(), bpRelation.get_ValueAsInt(I_C_BP_Relation.COLUMNNAME_C_BPartnerRelation_ID), order.get_TrxName());
		}
		if (billBPartner != null) {
			order.setBill_BPartner_ID(billBPartner.get_ID());
		}
		// Openup Solutions - #13739 - End




		//	Set default price list
		int currencyId = commissionRun.get_ValueAsInt("C_Currency_ID");
		if(currencyId == 0) {
			currencyId = commissionDefinition.getC_Currency_ID();
		}
		MCurrency currency = MCurrency.get(getCtx(), currencyId);
		MPriceList defaultPriceList = null;
		if (commissionDefinition.get_ValueAsBoolean("IsTaxIncluded")) {
			defaultPriceList = MPriceList.get(getCtx(), isSOTrx(), currency.getISO_Code(), true);
		} else {
			defaultPriceList = MPriceList.getDefault(getCtx(), isSOTrx(), currency.getISO_Code());
		}
		if(defaultPriceList != null) {
			order.setM_PriceList_ID(defaultPriceList.getM_PriceList_ID());
		} else {
			throw new IllegalArgumentException("@DefaultPriceListCurrencyNotFound@ (@C_Currency_ID@: " + currency.getISO_Code() + ")");	//	TODO Translate it: CommissionAPInvoice - Currency of PO Price List not Commission Currency
		}
		order.setSalesRep_ID(getAD_User_ID());	//	caller
		order.setDateOrdered(getDateOrdered());
		order.setDocStatus(MOrder.DOCSTATUS_Drafted);
		order.setDocAction(MOrder.DOCACTION_Complete);
		//
		order.setDescription(Msg.parseTranslation(getCtx(), "@Generate@: @C_CommissionRun_ID@ " + commissionRun.getDocumentNo()));
		//	
		if(commissionRun.get_ValueAsInt("C_Order_ID") > 0) {
			MOrder sourceOrder = new MOrder(getCtx(), commissionRun.get_ValueAsInt("C_Order_ID"), get_TrxName());
			order.addDescription(Msg.parseTranslation(getCtx(), "@C_Order_ID@ " + sourceOrder.getDocumentNo()));
			//	Set Project
			if(sourceOrder.getC_Project_ID() > 0) {
				order.setC_Project_ID(sourceOrder.getC_Project_ID());
			}
		}
		//	Set Contract
		if(commissionRun.get_ValueAsInt("S_Contract_ID") > 0) {
			order.set_ValueOfColumn("S_Contract_ID", commissionRun.get_ValueAsInt("S_Contract_ID"));
		}
		//	Add Project Reference to Split
		if(order.getC_Project_ID() == 0
				&& commissionRun.get_ValueAsInt("C_Project_ID") > 0) {
			order.setC_Project_ID(commissionRun.get_ValueAsInt("C_Project_ID"));
		}
		//	Set commission Run
		order.set_ValueOfColumn("C_CommissionRun_ID", commissionRun.getC_CommissionRun_ID());
		//		
		order.saveEx();
		orders.put(bPartnerId, order);
		//	Add to message
		created++;
		addToMessage(order.getDocumentNo());
		return order;
	}
	
	/**
	 * Create line from invoice
	 * @param commissionAmt
	 * @param order
	 * @param chargeId
	 */
	private void createOrderLine(MCommissionAmt commissionAmt, MOrder order, int chargeId, int productId) {
		//	Create Invoice Line
 		MOrderLine orderLine = new MOrderLine(order);
 		if(productId > 0) {
 			MProduct product = MProduct.get(getCtx(), productId);
 			orderLine.setProduct(product);
 			
 		} else {
 			orderLine.setC_Charge_ID(chargeId);
 		}
 		orderLine.setQty(Env.ONE);
		orderLine.setPrice(commissionAmt.getCommissionAmt());
		orderLine.setTax();
		orderLine.saveEx();
	}


	/**
	 * Add Document Info for message to return
	 * @param documentInfo
	 */
	private void addToMessage(String documentInfo) {
		if(generatedDocuments.length() > 0) {
			generatedDocuments.append(", ");
		}
		//	
		generatedDocuments.append(documentInfo);
	}
}
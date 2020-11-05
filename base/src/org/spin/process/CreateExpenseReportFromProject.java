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

import java.math.BigDecimal;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MTimeExpense;
import org.compiere.model.MTimeExpenseLine;
import org.compiere.model.X_S_TimeExpense;
import org.compiere.util.Env;

/** Generated Process for (Create Expense Report from Drop Ship Orders)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.1
 */
public class CreateExpenseReportFromProject extends CreateExpenseReportFromProjectAbstract {
	/**	Expense Report	*/
	private MTimeExpense expenseReport = null;
	/**	Result	*/
	private StringBuffer result = new StringBuffer();
	/**	Counter	*/
	private int created = 0;
	
	@Override
	protected String doIt() throws Exception {
		//	Loop for keys
		for(Integer key : getSelectionKeys()) {
			int orderLineId = getSelectionAsInt(key, "SOL_C_OrderLine_ID");
			int linkOrderLineId = getSelectionAsInt(key, "SOL_Link_OrderLine_ID");
			int expenseProductId = getSelectionAsInt(key, "SOL_M_Product_ID");
			boolean isReleaseOrderBalance = getSelectionAsBoolean(key, "SOL_IsReleaseOrderBalance");
			BigDecimal qty = getSelectionAsBigDecimal(key, "SOL_QtyToDeliver");
			MOrderLine orderLine = new MOrderLine(getCtx(), orderLineId, get_TrxName());

			//Openup. Nicolas Sarlabos. 21/10/2020. #14905.
			if(!orderLine.getC_Order().isSOTrx()){
				linkOrderLineId = getSelectionAsInt(key, "SOL_C_OrderLine_ID");
				orderLineId = getSelectionAsInt(key, "SOL_Link_OrderLine_ID");

				//Openup. Nicolas Sarlabos. 04/11/2020. #14990.
				if(orderLineId > 0){
					orderLine = new MOrderLine(getCtx(), orderLineId, get_TrxName());
				} else orderLine = new MOrderLine(getCtx(), linkOrderLineId, get_TrxName());
				//Fin #14990.
			}//Fin #14905.

			if(isReleaseOrderBalance) {
				BigDecimal qtyDelivered = orderLine.getQtyDelivered();
				BigDecimal qtyOrdered = orderLine.getQtyOrdered();
				if(qtyDelivered == null) {
					qtyDelivered = Env.ZERO;
				}
				BigDecimal qtyAvailable = qtyOrdered.subtract(qtyDelivered);
				orderLine.set_ValueOfColumn("ReleasedQty", qtyAvailable.subtract(qty));
				orderLine.saveEx();
			}

			if(qty.compareTo(Env.ZERO)==0){
				orderLine.setQtyLostSales(orderLine.getQtyReserved());
				orderLine.setQtyReserved(Env.ZERO);
				orderLine.saveEx();
			}

			MOrder order = orderLine.getParent();
			//	Create new
			if(expenseReport == null) {
				processDocument();
				createExpenseReport(order);
			}
			//	Set Quantity
			BigDecimal priceActual = getSelectionAsBigDecimal(key, "SOL_PriceActual");
			if(priceActual == null
					|| priceActual == Env.ZERO) {
				priceActual = orderLine.getPriceActual();
			}
			MTimeExpenseLine expenseLine = new MTimeExpenseLine(getCtx(), 0, get_TrxName());
			expenseLine.setS_TimeExpense_ID(expenseReport.getS_TimeExpense_ID());
			expenseLine.setDateExpense(getDateReport());
			expenseLine.setIsTimeReport(false);
			expenseLine.setIsInvoiced(false);
			expenseLine.setC_Currency_ID(expenseReport.getC_Currency_ID());
			//	Validate product
			expenseLine.setM_Product_ID(expenseProductId);
			expenseLine.setQty(qty);
			expenseLine.setExpenseAmt(priceActual);
			if(order.get_ValueAsInt("S_Contract_ID") > 0) {
				expenseLine.set_ValueOfColumn("S_ContractLine_ID", order.get_ValueAsInt("S_Contract_ID"));
			}
			if(order.getC_Project_ID() > 0) {
				expenseLine.setC_Project_ID(order.getC_Project_ID());
			}
			expenseLine.setC_OrderLine_ID(orderLineId);
			if(orderLine.getC_ProjectPhase_ID() > 0) {
				expenseLine.setC_ProjectPhase_ID(orderLine.getC_ProjectPhase_ID());
			}
			if(orderLine.getC_ProjectTask_ID() > 0) {
				expenseLine.setC_ProjectTask_ID(orderLine.getC_ProjectTask_ID());
			}
			if(linkOrderLineId > 0) {
				MOrderLine linkOrderLine = new MOrderLine(getCtx(), linkOrderLineId, get_TrxName());
				MOrder linkOrder = linkOrderLine.getParent();
				if(!linkOrder.getDocStatus().equals(MOrder.ACTION_Complete)) {
					throw new AdempiereException("@Link_OrderLine_ID@ @InvoiceCreateDocNotCompleted@");
				}
				if(isReleaseOrderBalance) {
					BigDecimal qtyDelivered = linkOrderLine.getQtyDelivered();
					BigDecimal qtyOrdered = linkOrderLine.getQtyOrdered();
					if(qtyDelivered == null) {
						qtyDelivered = Env.ZERO;
					}
					BigDecimal qtyAvailable = qtyOrdered.subtract(qtyDelivered);
					linkOrderLine.set_ValueOfColumn("ReleasedQty", qtyAvailable.subtract(qty));
					linkOrderLine.saveEx();
				}
				expenseLine.set_ValueOfColumn("Link_OrderLine_ID", linkOrderLineId);
			}
			expenseLine.saveEx();
		}
		//	Last
		processDocument();
		
		return getDocumentResult();
	}
	
	/**
	 * Process Expense
	 */
	private void processDocument() {
		if(expenseReport == null) {
			return;
		}
		//	Process Selection
		if(!expenseReport.processIt(getDocAction())) {
			throw new AdempiereException("@Error@ " + expenseReport.getProcessMsg());
		}
		//	
		expenseReport.saveEx();
		addDocumentResult(expenseReport.getDocumentNo());
		addLog(expenseReport.getS_TimeExpense_ID(), null, null, expenseReport.toString());
	}
	
	/**
	 * Create Expense Report
	 * @param order
	 */
	private void createExpenseReport(MOrder order) {
		expenseReport = new MTimeExpense(getCtx(), 0, get_TrxName());
		expenseReport.setDocStatus(X_S_TimeExpense.DOCSTATUS_Drafted);
		expenseReport.setDocAction(X_S_TimeExpense.DOCACTION_Complete);
		expenseReport.setC_BPartner_ID(order.getC_BPartner_ID());
		expenseReport.setDateReport(getDateReport());
		expenseReport.setM_Warehouse_ID(order.getM_Warehouse_ID());
		expenseReport.setM_PriceList_ID(order.getM_PriceList_ID());
		if(order.get_ValueAsInt("S_Contract_ID") > 0) {
			expenseReport.set_ValueOfColumn("S_Contract_ID", order.get_ValueAsInt("S_Contract_ID"));
		}
		expenseReport.set_ValueOfColumn("IsNotGenerateReceipt", getIsNotGenerateReceipt());//Openup. Nicolas Sarlabos. 16/09/2020. #14415.
		expenseReport.set_ValueOfColumn("IsNotGenerateDelivery", getIsNotGenerateDelivery());//Openup. Nicolas Sarlabos. 21/10/2020. #14905.
		expenseReport.saveEx();
	}
	
	/**
	 * Add document to result
	 * @param documentNo
	 */
	private void addDocumentResult(String documentNo) {
		created++;
		//	Add message
		if(result.length() > 0) {
			result.append(", ");
		}
		result.append(documentNo);
	}
	
	/**
	 * Get Document Result
	 * @return
	 */
	private String getDocumentResult() {
		//	Add message
		String returnMessage = "@Created@: " + created;
		if(result.length() > 0) {
			returnMessage = "@Created@: " + created + " [" + result + "]";
		}
		return returnMessage;
	}
}
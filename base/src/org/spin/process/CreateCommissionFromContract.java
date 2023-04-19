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

import java.util.ArrayList;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_C_Commission;
import org.compiere.model.I_C_CommissionLine;
import org.compiere.model.I_C_CommissionType;
import org.compiere.model.I_C_Project;
import org.compiere.model.MCommission;
import org.compiere.model.MCommissionRun;
import org.compiere.model.MDocType;
import org.compiere.model.MProject;
import org.compiere.model.Query;
import org.compiere.util.Msg;

import com.eevolution.model.I_S_Contract;
import com.eevolution.model.MSContract;
import com.eevolution.model.X_S_Contract;
import java.util.List;

/** Generated Process for (Create Commission from Contract)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.1
 */
public class CreateCommissionFromContract extends CreateCommissionFromContractAbstract {

	/**	Result	*/
	private StringBuffer result = new StringBuffer();
	/**	Counter	*/
	private int created = 0;
	
	@Override
	protected String doIt() throws Exception {
		if(getRecord_ID() <= 0) {
			generateCommissionForAll();
		} else if(getTable_ID() == I_S_Contract.Table_ID) {
			generateCommissionForContract(new MSContract(getCtx(), getRecord_ID(), get_TrxName()));
		} else if(getTable_ID() == I_C_Project.Table_ID) {
			generateCommissionForProject(new MProject(getCtx(), getRecord_ID(), get_TrxName()));
		}
		return getDocumentResult();
	}
	
	/**
	 * generate commission for all contract of customer
	 */
	private void generateCommissionForAll() {
		List<Object> parameters = new ArrayList<>();
		String whereClause = I_S_Contract.COLUMNNAME_DocStatus + " = ?";
		parameters.add(X_S_Contract.DOCSTATUS_Completed);
		if(getBPartnerId() > 0) {
			whereClause = " AND " + I_S_Contract.COLUMNNAME_C_BPartner_ID + " = ?";
			parameters.add(getBPartnerId());
		}
		new Query(getCtx(), I_S_Contract.Table_Name, whereClause, get_TrxName())
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.setParameters(parameters)
			.setOrderBy(I_S_Contract.COLUMNNAME_DateDoc)
			.<MSContract>list().forEach(contract -> {
				generateCommissionForContract(contract);
		});
	}
	
	/**
	 * Generate Commission for a specific contract
	 * @param contract
	 */
	private void generateCommissionForContract(X_S_Contract contract) {
		//	Get from project
		new Query(getCtx(), I_C_Project.Table_Name, I_S_Contract.COLUMNNAME_S_Contract_ID + " = ?", get_TrxName())
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.setParameters(contract.getS_Contract_ID())
			.setOrderBy(I_C_Project.COLUMNNAME_DateStart)
			.<MProject>list().forEach(project -> {
				generateCommissionForProject(project);
		});
	}
	
	
	/**
	 * Create commission for a specific project
	 * @param project
	 */
	private void generateCommissionForProject(MProject project) {
		X_S_Contract contract = new X_S_Contract(getCtx(), project.get_ValueAsInt(I_S_Contract.COLUMNNAME_S_Contract_ID), get_TrxName());
		//	Generate
		new Query(getCtx(), I_C_Commission.Table_Name, I_C_CommissionType.COLUMNNAME_C_CommissionType_ID + " = ? ", get_TrxName())
			.setOnlyActiveRecords(true)
			.setParameters(getCommissionTypeId())
			.<MCommission>list()
			.stream()
			.filter(mCommission -> {
				// (This is for Split) If C_Commission.DocBasisType = S (Division), this needs to be linked to the contract to create the CRun
				if ("S".equalsIgnoreCase(mCommission.getDocBasisType())) {
					String query = I_C_CommissionLine.COLUMNNAME_C_Commission_ID + "=? AND S_Contract_ID=? AND " + I_C_CommissionLine.COLUMNNAME_IsActive + "=?";
					return new Query(getCtx(), I_C_CommissionLine.Table_Name, query, get_TrxName())
							.setParameters(mCommission.get_ID(), contract.get_ID(), true)
							.match();
				} else {
					return true;
				}
			})
			.forEach(commissionDefinition -> {
				if(getDocTypeId() <= 0) {
					setDocTypeId(MDocType.getDocType(MDocType.DOCBASETYPE_SalesCommission, project.getAD_Org_ID()));
				}
				MCommissionRun commissionRun = new MCommissionRun(commissionDefinition);
				commissionRun.setDateDoc(getDateDoc());
				commissionRun.setC_DocType_ID(getDocTypeId());
				commissionRun.setDescription(Msg.parseTranslation(getCtx(), "@Generate@: @S_Contract_ID@ - " + contract.getDocumentNo() + " @C_Project_ID@: " + project.getValue()));
				commissionRun.setAD_Org_ID(project.getAD_Org_ID());
				commissionRun.set_ValueOfColumn(I_S_Contract.COLUMNNAME_S_Contract_ID, contract.getS_Contract_ID());
				commissionRun.set_ValueOfColumn(I_C_Project.COLUMNNAME_C_Project_ID, project.getC_Project_ID());
				// Openup Solutions - #14207 - Raul Capecce - Requerido para establecer moneda destino del Calculo de Comision
				commissionRun.set_ValueOfColumn(I_C_Project.COLUMNNAME_C_Currency_ID, project.getC_Currency_ID());
				// Openup Solutions - #14207 - End

				//	Set filter value a project
				commissionRun.addFilterValues(I_S_Contract.COLUMNNAME_S_Contract_ID, contract.getS_Contract_ID());
				commissionRun.addFilterValues(I_C_Project.COLUMNNAME_C_Project_ID, project.getC_Project_ID());
				commissionRun.saveEx();
				//	Process commission
				commissionRun.setDocStatus(MCommissionRun.DOCSTATUS_Drafted);
				//	Complete
				if(commissionRun.processIt(MCommissionRun.DOCACTION_Complete)) {
					addDocumentResult(commissionRun.getDocumentNo());
				} else {
					throw new AdempiereException(commissionRun.getProcessMsg());
				}
				//	Add to log
				addLog(commissionRun.getC_CommissionRun_ID(), null, null, "@C_Project_ID@: " + project.getValue() + " @C_CommissionRun_ID@: " + commissionRun.getDocumentNo() + " @Created@");
		});
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
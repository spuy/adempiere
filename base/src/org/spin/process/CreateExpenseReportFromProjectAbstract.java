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

import java.sql.Timestamp;
import org.compiere.process.SvrProcess;

/** Generated Process for (Create Expense Report from Drop Ship Orders)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.1
 */
public abstract class CreateExpenseReportFromProjectAbstract extends SvrProcess {
	/** Process Value 	*/
	private static final String VALUE_FOR_PROCESS = "DropShipOrdersCreateExpense";
	/** Process Name 	*/
	private static final String NAME_FOR_PROCESS = "Create Expense Report from Drop Ship Orders";
	/** Process Id 	*/
	private static final int ID_FOR_PROCESS = 1000045;
	/**	Parameter Name for Report Date	*/
	public static final String DATEREPORT = "DateReport";
	/**	Parameter Name for Document Action	*/
	public static final String DOCACTION = "DocAction";
	/**	Parameter Value for Report Date	*/
	private Timestamp dateReport;
	/**	Parameter Value for Document Action	*/
	private String docAction;
	//Openup. Nicolas Sarlabos. 16/09/2020. #14415.
	public static final String ISNOTGENERATERECEIPT = "IsNotGenerateReceipt";
	private boolean isNotGenerateReceipt;
	//Fin #14415.
	//Openup. Nicolas Sarlabos. 21/10/2020. #14905.
	public static final String ISNOTGENERATEDELIVERY = "IsNotGenerateDelivery";
	private boolean isNotGenerateDelivery;
	//Fin #14905.

	@Override
	protected void prepare() {
		dateReport = getParameterAsTimestamp(DATEREPORT);
		docAction = getParameterAsString(DOCACTION);
		isNotGenerateReceipt = getParameterAsBoolean(ISNOTGENERATERECEIPT);//Openup. Nicolas Sarlabos. 16/09/2020. #14415.
		isNotGenerateDelivery = getParameterAsBoolean(ISNOTGENERATEDELIVERY);//Openup. Nicolas Sarlabos. 21/10/2020. #14905.
	}

	/**	 Getter Parameter Value for Report Date	*/
	protected Timestamp getDateReport() {
		return dateReport;
	}

	/**	 Setter Parameter Value for Report Date	*/
	protected void setDateReport(Timestamp dateReport) {
		this.dateReport = dateReport;
	}

	/**	 Getter Parameter Value for Document Action	*/
	protected String getDocAction() {
		return docAction;
	}

	protected boolean getIsNotGenerateReceipt() { return isNotGenerateReceipt; }//Openup. Nicolas Sarlabos. 16/09/2020. #14415.

	protected boolean getIsNotGenerateDelivery() { return isNotGenerateDelivery; }//Openup. Nicolas Sarlabos. 21/10/2020. #14905.

	/**	 Setter Parameter Value for Document Action	*/
	protected void setDocAction(String docAction) {
		this.docAction = docAction;
	}

	/**	 Getter Parameter Value for Process ID	*/
	public static final int getProcessId() {
		return ID_FOR_PROCESS;
	}

	/**	 Getter Parameter Value for Process Value	*/
	public static final String getProcessValue() {
		return VALUE_FOR_PROCESS;
	}

	/**	 Getter Parameter Value for Process Name	*/
	public static final String getProcessName() {
		return NAME_FOR_PROCESS;
	}
}
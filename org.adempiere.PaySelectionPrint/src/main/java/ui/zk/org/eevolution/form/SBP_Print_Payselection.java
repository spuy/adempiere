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

package org.eevolution.form;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.X_C_BankAccountDoc_Check;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.window.FDialog;
import org.adempiere.webui.window.SimplePDFViewer;
import org.compiere.model.I_C_PaySelection;
import org.compiere.model.MBankAccount;
import org.compiere.model.MDocType;
import org.compiere.model.MLookup;
import org.compiere.model.MPaySelection;
import org.compiere.model.MPaySelectionCheck;
import org.compiere.model.MPaySelectionLine;
import org.compiere.model.MPayment;
import org.compiere.model.MQuery;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.model.X_C_BankAccountDoc;
import org.compiere.model.X_C_Payment;
import org.compiere.print.ReportEngine;
import org.compiere.process.DocAction;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.PaymentExport;
import org.compiere.util.PaymentExportList;
import org.compiere.util.Util;
//import org.globalqss.model.MLCOInvoiceWithholding;
import org.zkoss.zul.Filedownload;

/** Generated Process for (Print_Payselection)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.0
 */
public class SBP_Print_Payselection extends SBP_Print_PayselectionAbstract
{
	private List<File> pdfList = new ArrayList<>();
	private boolean isSingleRecord = false;
	private boolean isPrintPayment = false;
	
	private List<MPaySelectionCheck> paySelectionChecks = null;
	private List<MPaySelectionCheck> paySelectionChecksToProcess = null;
	private int paySelectionId = 0;
	private MPaySelection paySelection = null;
	
	private List<MPayment>	paymentList					= new ArrayList<MPayment>();
	protected CLogger			log = CLogger.getCLogger (getClass());
	@Override
	protected void prepare()
	{
		super.prepare();
		if ( getRecord_ID() !=0 && getTable_ID() == MPaySelection.Table_ID) {
			paySelection = new MPaySelection(getCtx(), paySelectionId, get_TrxName());
			paySelectionId = getRecord_ID();
			isSingleRecord = true;
		}
		if (getRecord_ID() != 0 && getTable_ID()==MPayment.Table_ID) {
			isPrintPayment = true;
		}
		
	}

	@Override
	protected String doIt() throws Exception
	{
		paySelectionChecks = (List<MPaySelectionCheck>) getInstancesForSelection(get_TrxName());
		if (getPayPrint().equals("PR"))
			cmd_print();
		else if (getPayPrint().equals("RR"))
			cmd_reprint();
		else if (getPayPrint().equals("EX"))
			cmd_export();
		else if (getPayPrint().equals("EFT"))
			cmd_EFT();
		else if (getPayPrint().equals("CC"))
			cmd_CreateReceipt();
		return "";
	}
	
	private void printSingleCheck(){}

private void addPDFFile(File file)
{
	pdfList.add(file);
}
	
	private void cmd_print()
	{
		//	for all checks
		pdfList = new ArrayList<>();
		if (isSingleRecord) {
			cmd_printPayselection();
			return;				
		}
		SimplePDFViewer chequeViewer = null;
		for (MPaySelectionCheck paySelectionCheck:paySelectionChecks) {				

			ReportEngine re = ReportEngine.get(Env.getCtx(), ReportEngine.CHECK, paySelectionCheck.get_ID());
			try 
			{
				File file = File.createTempFile("WPayPrint", null);
				addPDFFile(re.getPDF(file));
			}
			catch (Exception e)
			{
				log.log(Level.SEVERE, e.getLocalizedMessage(), e);
				return;
			}
			confirmPrint (paySelectionCheck, getPaymentRule());	
			StringBuffer sb = new StringBuffer();
			X_C_BankAccountDoc bankAccountDoc = new X_C_BankAccountDoc(getCtx(), getBankAccountDocId(), get_TrxName());
			int currentNext = bankAccountDoc.getCurrentNext();
			currentNext = currentNext + 1;
			sb.append("UPDATE C_BankAccountDoc SET CurrentNext=").append(currentNext)
			.append(" WHERE C_BankAccount_ID=").append(paySelectionCheck.getC_PaySelection().getC_BankAccount_ID())
			.append(" AND PaymentRule='").append(getPaymentRule()).append("'");
			DB.executeUpdate(sb.toString(), null);
			

		}
		if (getCheckPrintOutput().equals("02")){
			try 
			{
				File outFile = File.createTempFile("WPayPrint", null);
				AEnv.mergePdf(pdfList, outFile);
				chequeViewer = new SimplePDFViewer("Pay and Print", new FileInputStream(outFile));
				chequeViewer.setAttribute(Window.MODE_KEY, Window.MODE_EMBEDDED);
				chequeViewer.setWidth("100%");
				if (chequeViewer != null)
					SessionManager.getAppDesktop().showWindow(chequeViewer);

			}
			catch (Exception e)
			{
				log.log(Level.SEVERE, e.getLocalizedMessage(), e);
				return;
			}
		}
		else if (getCheckPrintOutput().equals("03")) {
			commit();
			String whereClause = "C_Payment_ID in (";
			int counter = 0;
			for (MPaySelectionCheck paySelectionCheck:paySelectionChecks) {
				whereClause = whereClause + paySelectionCheck.getC_Payment_ID() + ',';
				counter++;
			}
			whereClause = whereClause.substring(0, whereClause.length()-1) + ")";
			MQuery zoomQuery = new MQuery();   //  ColumnName might be changed in MTab.validateQuery
            String column = MPayment.COLUMNNAME_C_Payment_ID;						
				zoomQuery.setZoomColumnName(column);
				//remove _ID to get table name
				zoomQuery.setZoomTableName(MPayment.Table_Name);
				zoomQuery.setTableName(MPayment.Table_Name);
			zoomQuery.addRestriction(whereClause);
			zoomQuery.setRecordCount(counter);    //  guess
			AEnv.zoom(zoomQuery);
        }





		//	Update BankAccountDoc
		
		SimplePDFViewer remitViewer = null; 
				if (isPrintRemittance())
				{
					pdfList = new ArrayList<>();
					paySelectionChecks.stream()
							.filter(paySelectionCheck -> paySelectionCheck != null)
							.forEach(paySelectionCheck -> {
						ReportEngine re = ReportEngine.get(Env.getCtx(), ReportEngine.REMITTANCE, paySelectionCheck.get_ID());
						try 
						{
							File file = File.createTempFile("WPayPrint", null);
							addPDFFile(re.getPDF(file));
						}
						catch (Exception e)
						{
							log.log(Level.SEVERE, e.getLocalizedMessage(), e);
						}
					});

					try
					{
						File outFile = File.createTempFile("WPayPrint", null);
						AEnv.mergePdf(pdfList, outFile);
						String name = Msg.translate(Env.getCtx(), "Remittance");
						remitViewer = new SimplePDFViewer("Print " + " - " + name, new FileInputStream(outFile));
						remitViewer.setAttribute(Window.MODE_KEY, Window.MODE_EMBEDDED);
						remitViewer.setWidth("100%");
					}
					catch (Exception e)
					{
						log.log(Level.SEVERE, e.getLocalizedMessage(), e);
					}
				}	//	remittance



		//if (remitViewer != null)
		//	SessionManager.getAppDesktop().showWindow(remitViewer);





	}   //  cmd_print
	
	
	private void cmd_printPayselection()
	{
		//	for all checks
		pdfList = new ArrayList<>();
		paySelectionChecks = new ArrayList<MPaySelectionCheck>();
		String sql = "Select distinct paymentRule from C_PayselectionCheck where C_PaySelection_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, paySelectionId);
			rs = pstmt.executeQuery();
			while (rs.next()){
				
				String paymentRule = rs.getString(1);
				log.info(paymentRule);
				if (!getChecks(paymentRule))
					return;
				if (paySelectionChecks.isEmpty())
					continue;

				//	for all checks
				pdfList = new ArrayList<>();
				paySelectionChecksToProcess.stream().filter(paySelectionCheck -> paySelectionCheck != null).forEach(paySelectionCheck -> {
					//	ReportCtrl will check BankAccountDoc for PrintFormat
					ReportEngine re = ReportEngine.get(Env.getCtx(), ReportEngine.CHECK, paySelectionCheck.get_ID());
					try 
					{
						File file = File.createTempFile("WPayPrint", null);
						addPDFFile(re.getPDF(file));
					}
					catch (Exception e)
					{
						log.log(Level.SEVERE, e.getLocalizedMessage(), e);
						return;
					}
				});
				
				SimplePDFViewer chequeViewer = null;
				try 
				{
					File outFile = File.createTempFile("WPayPrint", null);
					AEnv.mergePdf(pdfList, outFile);
					chequeViewer = new SimplePDFViewer("Pay and Print", new FileInputStream(outFile));
					chequeViewer.setAttribute(Window.MODE_KEY, Window.MODE_EMBEDDED);
					chequeViewer.setWidth("100%");
				}
				catch (Exception e)
				{
					log.log(Level.SEVERE, e.getLocalizedMessage(), e);
					return;
				}

				//	Update BankAccountDoc
				int lastDocumentNo = MPaySelectionCheck.confirmPrint (paySelectionChecksToProcess, null);
				if (lastDocumentNo != 0)
				{
					StringBuffer sb = new StringBuffer();
					sb.append("UPDATE C_BankAccountDoc SET CurrentNext=").append(++lastDocumentNo)
						.append(" WHERE C_BankAccount_ID=").append(paySelection.getC_BankAccount_ID())
						.append(" AND PaymentRule='").append(paymentRule).append("'");
					DB.executeUpdate(sb.toString(), null);
				}

				SimplePDFViewer remitViewer = null; 
				if (FDialog.ask(0, null, "VPayPrintPrintRemittance"))
				{
					pdfList = new ArrayList<>();
					paySelectionChecks.stream()
							.filter(paySelectionCheck -> paySelectionCheck != null)
							.forEach(paySelectionCheck -> {
						ReportEngine re = ReportEngine.get(Env.getCtx(), ReportEngine.REMITTANCE, paySelectionCheck.get_ID());
						try 
						{
							File file = File.createTempFile("WPayPrint", null);
							addPDFFile(re.getPDF(file));
						}
						catch (Exception e)
						{
							log.log(Level.SEVERE, e.getLocalizedMessage(), e);
						}
					});
					
					try
					{
						File outFile = File.createTempFile("WPayPrint", null);
						AEnv.mergePdf(pdfList, outFile);
						String name = Msg.translate(Env.getCtx(), "Remittance");
						remitViewer = new SimplePDFViewer("Print " + " - " + name, new FileInputStream(outFile));
						remitViewer.setAttribute(Window.MODE_KEY, Window.MODE_EMBEDDED);
						remitViewer.setWidth("100%");
					}
					catch (Exception e)
					{
						log.log(Level.SEVERE, e.getLocalizedMessage(), e);
					}
				}	//	remittance

				pdfList = new ArrayList<>();
				
				if (chequeViewer != null)
					SessionManager.getAppDesktop().showWindow(chequeViewer);
				
				if (remitViewer != null)
					SessionManager.getAppDesktop().showWindow(remitViewer);
			
				
			}
				
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "allocatePaySelection", e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		
	}   //  cmd_print
	
	private boolean getChecks(String paymentRule)
	{
		//  get data
		paySelectionChecksToProcess.clear();
		int startDocumentNo = 1;
		AtomicInteger docNo = new AtomicInteger(startDocumentNo);
		log.config("C_PaySelection_ID=" +  ", PaymentRule=" +  paymentRule + ", DocumentNo=" + startDocumentNo);
		//
		//	get Selections
		List<MPaySelectionCheck> localdraftedPayseleChecks = paySelectionChecks;
		localdraftedPayseleChecks.stream()
		.filter(paySelectionCheck -> paySelectionCheck != null
		&& paySelectionCheck.getC_Payment_ID() == 0 
		&& paySelectionCheck.getPaymentRule().equals(paymentRule)).forEach(paySelectionCheck -> {
			paySelectionCheck.setDocumentNo(String.valueOf(docNo.get()));
			docNo.updateAndGet(no -> no + 1);
			paySelectionCheck.saveEx();
			paySelectionChecksToProcess.add(paySelectionCheck);
		});

		//
		//paymentBatch = MPaymentBatch.getForPaySelection (Env.getCtx(), paySelectionId, null);
		return true;
	}   //  getChecks
	
	private int getChecksForReprint(String paymentRule)
	{
		//  get data
		paySelectionChecksToProcess.clear();
		int startDocumentNo = 1;
		AtomicInteger docNo = new AtomicInteger(startDocumentNo);
		log.config("C_PaySelection_ID=" +  ", PaymentRule=" +  paymentRule + ", DocumentNo=" + startDocumentNo);
		//
		//	get Selections
		List<MPaySelectionCheck> localdraftedPayseleChecks = paySelectionChecks;
		localdraftedPayseleChecks.stream()
		.filter(paySelectionCheck -> paySelectionCheck != null
		&& paySelectionCheck.getC_Payment_ID() != 0 
		&& paySelectionCheck.getPaymentRule().equals(paymentRule)).forEach(paySelectionCheck -> {
			paySelectionCheck.setDocumentNo(String.valueOf(docNo.get()));
			docNo.updateAndGet(no -> no + 1);
			paySelectionCheck.saveEx();
			paySelectionChecksToProcess.add(paySelectionCheck);
		});

		//
		//paymentBatch = MPaymentBatch.getForPaySelection (Env.getCtx(), paySelectionId, null);
		return docNo.intValue();
	}   //  getChecks
	
	private void cmd_reprint()
	{
		//	for all checks
		pdfList = new ArrayList<>();
		paySelectionChecks = new ArrayList<MPaySelectionCheck>();
		String sql = "Select distinct paymentRule from C_PayselectionCheck where C_PaySelection_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, paySelectionId);
			rs = pstmt.executeQuery();
			while (rs.next()){
				
				String paymentRule = rs.getString(1);
				log.info(paymentRule);
				int lastDocumentNo = getChecksForReprint(paymentRule);
				if (paySelectionChecks.isEmpty())
					continue;

				//	for all checks
				pdfList = new ArrayList<>();
				paySelectionChecksToProcess.stream().filter(paySelectionCheck -> paySelectionCheck != null).forEach(paySelectionCheck -> {
					//	ReportCtrl will check BankAccountDoc for PrintFormat
					ReportEngine re = ReportEngine.get(Env.getCtx(), ReportEngine.CHECK, paySelectionCheck.get_ID());
					try 
					{
						File file = File.createTempFile("WPayPrint", null);
						addPDFFile(re.getPDF(file));
					}
					catch (Exception e)
					{
						log.log(Level.SEVERE, e.getLocalizedMessage(), e);
						return;
					}
				});
				
				SimplePDFViewer chequeViewer = null;
				try 
				{
					File outFile = File.createTempFile("WPayPrint", null);
					AEnv.mergePdf(pdfList, outFile);
					chequeViewer = new SimplePDFViewer("Pay and Print", new FileInputStream(outFile));
					chequeViewer.setAttribute(Window.MODE_KEY, Window.MODE_EMBEDDED);
					chequeViewer.setWidth("100%");
				}
				catch (Exception e)
				{
					log.log(Level.SEVERE, e.getLocalizedMessage(), e);
					return;
				}

				//	Update BankAccountDoc
				if (lastDocumentNo != 0)
				{
					StringBuffer sb = new StringBuffer();
					sb.append("UPDATE C_BankAccountDoc SET CurrentNext=").append(++lastDocumentNo)
						.append(" WHERE C_BankAccount_ID=").append(paySelection.getC_BankAccount_ID())
						.append(" AND PaymentRule='").append(paymentRule).append("'");
					DB.executeUpdate(sb.toString(), null);
				}

				SimplePDFViewer remitViewer = null; 
				if (FDialog.ask(0, null, "VPayPrintPrintRemittance"))
				{
					pdfList = new ArrayList<>();
					paySelectionChecks.stream()
							.filter(paySelectionCheck -> paySelectionCheck != null)
							.forEach(paySelectionCheck -> {
						ReportEngine re = ReportEngine.get(Env.getCtx(), ReportEngine.REMITTANCE, paySelectionCheck.get_ID());
						try 
						{
							File file = File.createTempFile("WPayPrint", null);
							addPDFFile(re.getPDF(file));
						}
						catch (Exception e)
						{
							log.log(Level.SEVERE, e.getLocalizedMessage(), e);
						}
					});
					
					try
					{
						File outFile = File.createTempFile("WPayPrint", null);
						AEnv.mergePdf(pdfList, outFile);
						String name = Msg.translate(Env.getCtx(), "Remittance");
						remitViewer = new SimplePDFViewer("Print " + " - " + name, new FileInputStream(outFile));
						remitViewer.setAttribute(Window.MODE_KEY, Window.MODE_EMBEDDED);
						remitViewer.setWidth("100%");
					}
					catch (Exception e)
					{
						log.log(Level.SEVERE, e.getLocalizedMessage(), e);
					}
				}	//	remittance

				pdfList = new ArrayList<>();
				
				if (chequeViewer != null)
					SessionManager.getAppDesktop().showWindow(chequeViewer);
				
				if (remitViewer != null)
					SessionManager.getAppDesktop().showWindow(remitViewer);
			
				
			}
				
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "allocatePaySelection", e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		
	}   //  cmd_print
	
	private void cmd_EFT()
	{
		String sql = "Select distinct paymentRule from C_PayselectionCheck where C_PaySelection_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, paySelectionId);
			rs = pstmt.executeQuery();
			while (rs.next()){
				String paymentRule = rs.getString(1);
				log.info(paymentRule);
				if (!getChecks(paymentRule))
					return;
				if (paySelectionChecks.isEmpty())
					continue;				
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "allocatePaySelection", e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
	}   //  cmd_EFT
	
	private void cmd_export()
	{
		String sql = "Select distinct paymentRule from C_PayselectionCheck where paymentRule = ?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setString(1, getPaymentRule());
			rs = pstmt.executeQuery();
			while (rs.next()){
				try 
				{
					//  Get File Info
					File tempFile = File.createTempFile("paymentExport", ".txt");

					//  Create File
					int no = 0;
					StringBuffer error = new StringBuffer("");
					MBankAccount bankAccount = new MBankAccount(getCtx(), getBankAccountId(), get_TrxName());
					String paymentExportClass = bankAccount.getPaymentExportClass();
					if (paymentExportClass == null || paymentExportClass.trim().length() == 0) {
						paymentExportClass = "org.compiere.util.GenericPaymentExport";
					}
					//	Get Payment Export Class
					try
					{
						Class<?> clazz = Class.forName(paymentExportClass);
						if (PaymentExportList.class.isAssignableFrom(clazz))
						{
							PaymentExportList custom = (PaymentExportList)clazz.newInstance();
							no = custom.exportToFile(paySelectionChecks, tempFile, error);
							if(custom.getFile() != null) {
								tempFile = custom.getFile();
							}
						}
						else if (PaymentExport.class.isAssignableFrom(clazz))
						{
							PaymentExport custom = (PaymentExport)clazz.newInstance();
							no = custom.exportToFile(paySelectionChecks.toArray(new MPaySelectionCheck[paySelectionChecks.size()]), tempFile, error);
						}
					}
					catch (ClassNotFoundException e)
					{
						no = -1;
						error.append("No custom PaymentExport class " + paymentExportClass + " - " + e.toString());
						log.log(Level.SEVERE, error.toString(), e);
					}
					catch (Exception e)
					{
						no = -1;
						error.append("Error in " + paymentExportClass + " check log, " + e.toString());
						log.log(Level.SEVERE, error.toString(), e);
					}
					if (no >= 0) {
						Filedownload.save(new FileInputStream(tempFile), "plain/text", tempFile.getName());
						FDialog.info(0, null, "Saved",
								Msg.getMsg(Env.getCtx(), "NoOfLines") + "=" + no);

						if (FDialog.ask(0, null, "VPayPrintSuccess?"))
						{
							for (MPaySelectionCheck paySelectionCheck:paySelectionChecks) {
								confirmPrint(paySelectionCheck, getPaymentRule());
							}
							//	int lastDocumentNo = 
							//MPaySelectionCheck.confirmPrint (paySelectionChecksToProcess, null);
							//	document No not updated
						}
					} else {
						FDialog.error(0, null, "Error", error.toString());
					}
				}
				catch (Exception e) 
				{
					log.log(Level.SEVERE, e.getLocalizedMessage(), e);
				}		
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "allocatePaySelection", e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
	}   //  cmd_export
	
	private void cmd_CreateReceipt() {
		MPaySelectionCheck.confirmPrint(paySelectionChecks, null);		
		for (MPaySelectionCheck paySelectionCheck: paySelectionChecks) {
			MPayment payment = (MPayment)paySelectionCheck.getC_Payment();
			payment.setCheckNo(paySelectionCheck.get_ValueAsString(MPayment.COLUMNNAME_CheckNo));
			payment.saveEx();
			if (isisCreateDeposit()){
				MPayment receiptReference = payment;
				int	bankAccountFromId = receiptReference.getC_BankAccount_ID();
				MPayment inPayment = createPayment(receiptReference, getBankAccountId(), true, 
						receiptReference.getPayAmt(), receiptReference.getTenderType(),receiptReference.getDateTrx());
				inPayment.setPayAmt(receiptReference.getPayAmt());
				//	Set Reference
				receiptReference.setRef_Payment_ID(inPayment.getC_Payment_ID());
				receiptReference.saveEx();
				Boolean isPaymentCreated = true;
				//	Create out payment
				MPayment outPayment = createPayment(receiptReference, bankAccountFromId, false, 
						receiptReference.getPayAmt(), receiptReference.getTenderType(), receiptReference.getDateTrx());

				//	If is created a in payment then set reference here
				if(isPaymentCreated
						&& outPayment != null) {
					inPayment.setRef_Payment_ID(outPayment.getC_Payment_ID());
					inPayment.saveEx();
				}

				//	Complete Payments
				for(MPayment newpayment : paymentList) {
					newpayment.processIt(DocAction.ACTION_Complete);
					newpayment.saveEx();			
				}
			} 
		}
	}
	private MPayment createPayment(MPayment payment, int bankAccountId, boolean isReceipt, BigDecimal payAmt, String tenderType, Timestamp dateTrx) {
		MBankAccount bankAccount = MBankAccount.get(getCtx(), bankAccountId);
		MPayment payment_original = new MPayment(getCtx(), 0, get_TrxName());
	  	//	Set Value
		payment.setC_BPartner_ID(payment_original.getC_BPartner_ID());
		payment.setC_BankAccount_ID(bankAccountId);
		payment.setIsReceipt(isReceipt);
		payment.setTenderType(tenderType);
		payment.setDateTrx(dateTrx);
		payment.setDateAcct(dateTrx);
		if(!Util.isEmpty(payment_original.getDocumentNo())) {
			payment.setDocumentNo(payment_original.getDocumentNo());
		}
		payment.setC_Currency_ID(bankAccount.getC_Currency_ID());
		payment.setC_Charge_ID(payment_original.getC_Charge_ID());
		payment.setDocStatus(MPayment.DOCSTATUS_Drafted);
		if(payAmt != null) 
			payment.setPayAmt(payAmt);
		payment.setC_DocType_ID(isReceipt);
		payment.saveEx();
  	  	//	payment list
  	  	paymentList.add(payment);
		return payment;
	
	}
	
	private Boolean confirmPrint(MPaySelectionCheck paySelectionCheck,  String paymentRule) {
		AtomicInteger lastDocumentNo = new AtomicInteger();
		MPayment payment = new MPayment(paySelectionCheck.getCtx(), paySelectionCheck.getC_Payment_ID(), paySelectionCheck.get_TrxName());
		//	Existing Payment
		if (paySelectionCheck.getC_Payment_ID() != 0
				&& (payment.getDocStatus().equals(DocAction.STATUS_Completed)
						|| payment.getDocStatus().equals(DocAction.STATUS_Closed))) {
			//	Update check number
			if (getPaymentRule().equals(MPaySelectionCheck.PAYMENTRULE_Check)) {
				payment.setCheckNo(paySelectionCheck.getDocumentNo());
				String whereClause = "C_Payment_ID=?";
				X_C_BankAccountDoc_Check bankAccountDoc_Check = new Query(getCtx(), X_C_BankAccountDoc_Check.Table_Name, whereClause, get_TrxName())
						.setParameters(payment.getC_Payment_ID())
						.first();
				bankAccountDoc_Check.setisVoided(true);
				X_C_BankAccountDoc bankAccountDoc = (X_C_BankAccountDoc)bankAccountDoc_Check.getC_BankAccountDoc();
				createBankaccountDocCheck(payment);
			} else {
				payment.setDocumentNo(paySelectionCheck.getDocumentNo());
			}
			payment.set_CustomColumn(X_C_BankAccountDoc.COLUMNNAME_C_BankAccountDoc_ID, getBankAccountDocId());
			payment.saveEx();
		} else {	//	New Payment
			I_C_PaySelection paySelection =  paySelectionCheck.getC_PaySelection();
			MDocType documentType = MDocType.get(paySelectionCheck.getCtx(), paySelection.getC_DocType_ID());
			int docTypeId = documentType.getC_DocTypePayment_ID();
			//	
			payment = new MPayment(paySelectionCheck.getCtx(), 0, paySelectionCheck.get_TrxName());
			payment.setDateTrx(getDateTrx());
			payment.setDateAcct(getDateTrx());
			payment.setAD_Org_ID(paySelectionCheck.getAD_Org_ID());
			payment.set_CustomColumn("bpartnername", paySelectionCheck.get_Value("bpartnername"));
			payment.setC_BankAccount_ID(paySelection.getC_BankAccount_ID());
			payment.setAmount(paySelectionCheck.getParent().getC_Currency_ID(), paySelectionCheck.getPayAmt());
			payment.setDiscountAmt(paySelectionCheck.getDiscountAmt());
			payment.setDateTrx(paySelectionCheck.getParent().getPayDate());
			payment.setDateAcct(payment.getDateTrx()); // globalqss [ 2030685 ]
			payment.setWriteOffAmt(C_PayselectionCheck_calculateWithHolding(paySelectionCheck.getC_PaySelectionCheck_ID()));
			payment.setC_BPartner_ID(paySelectionCheck.getC_BPartner_ID());
			if (!paySelectionCheck.getPaymentRule().equals(MPaySelectionCheck.PAYMENTRULE_Check)) {
				payment.setDocumentNo(paySelectionCheck.getDocumentNo());
			}
			payment.saveEx();
			if (getPaymentRule().equals(MPaySelectionCheck.PAYMENTRULE_Check)) {
				payment.setBankCheck (paySelectionCheck.getParent().getC_BankAccount_ID(), false, paySelectionCheck.getDocumentNo());
			} else if (getPaymentRule().equals(MPaySelectionCheck.PAYMENTRULE_CreditCard)) {
				payment.setTenderType(X_C_Payment.TENDERTYPE_CreditCard);
			} else if (getPaymentRule().equals(MPaySelectionCheck.PAYMENTRULE_DirectDeposit)
					|| getPaymentRule().equals(MPaySelectionCheck.PAYMENTRULE_DirectDebit)) {
				payment.setBankACH(paySelectionCheck);
			} else {
				log.config("Unsupported Payment Rule=" + paySelectionCheck.getPaymentRule());
				throw  new AdempiereException("Unsupported Payment Rule=" + paySelectionCheck.getPaymentRule());
				//continue;
			}
			payment.setTrxType(X_C_Payment.TRXTYPE_CreditPayment);
			if (docTypeId > 0) {
				payment.setC_DocType_ID(docTypeId);
			}
			
			List<MPaySelectionLine> paySelectionLines = paySelectionCheck.getPaySelectionLinesAsList(false);
			log.config("confirmPrint - " + paySelectionCheck + " (#SelectionLines=" + (paySelectionLines != null? paySelectionLines.size(): 0) + ")");
			//	For bank Transfer
			if(documentType.isBankTransfer()) {
				payment.setC_Invoice_ID(-1);
				payment.setC_Order_ID(-1);
				payment.setTenderType(MPayment.TENDERTYPE_DirectDeposit);
				payment.saveEx();
				if(paySelectionLines != null) {
					for(MPaySelectionLine line : paySelectionLines) {
						if(line.getC_BankAccountTo_ID() == 0) {
							throw new AdempiereException("@C_BankAccountTo_ID@ @NotFound@");
						}
						//	For all
						MPayment receiptAccount = new MPayment(paySelectionCheck.getCtx(), 0, paySelectionCheck.get_TrxName());
						PO.copyValues(payment, receiptAccount);
						//	Set default values
						receiptAccount.setC_BankAccount_ID(line.getC_BankAccountTo_ID());
						receiptAccount.setIsReceipt(!payment.isReceipt());
						receiptAccount.setC_DocType_ID(!payment.isReceipt());
						receiptAccount.setRelatedPayment_ID(payment.getC_Payment_ID());
						receiptAccount.setTenderType(MPayment.TENDERTYPE_DirectDeposit);
						receiptAccount.setC_Charge_ID(line.getC_Charge_ID());
						receiptAccount.saveEx();
						receiptAccount.processIt(DocAction.ACTION_Complete);
						receiptAccount.saveEx();
						payment.setRelatedPayment_ID(receiptAccount.getC_Payment_ID());
						payment.setC_Charge_ID(receiptAccount.getC_Charge_ID());
						payment.saveEx();
					}
				}
			} else {
				//	Link to Invoice
				if (paySelectionCheck.getQty() == 1 && paySelectionLines != null && paySelectionLines.size() == 1) {
					MPaySelectionLine paySelectionLine = paySelectionLines.get(0);
					log.config("Map to Invoice " + paySelectionLine);
					//
					//	FR [ 297 ]
					//	For Order
					if(paySelectionLine.getC_Order_ID() != 0) {
						payment.setC_Order_ID (paySelectionLine.getC_Order_ID());
					}
					//	For Charge
					if (paySelectionLine.getC_Charge_ID() != 0) {
						payment.setC_Charge_ID(paySelectionLine.getC_Charge_ID());
						if (paySelectionLine.getHR_Movement_ID() > 0) {
							payment.setC_Project_ID(paySelectionLine.getHRMovement().getC_Project_ID());
						}
					}
					//	For Conversion Type
					if(paySelectionLine.getC_ConversionType_ID() != 0) {
						payment.setC_ConversionType_ID(paySelectionLine.getC_ConversionType_ID());
					}
					//	For Invoice
					if(paySelectionLine.getC_Invoice_ID() != 0) {
						payment.setC_Invoice_ID (paySelectionLine.getC_Invoice_ID());
					}
					//	For all
					payment.setIsPrepayment(paySelectionLine.isPrepayment());
					//	
					payment.setDiscountAmt (paySelectionLine.getDiscountAmt());
					payment.setWriteOffAmt(paySelectionLine.getDifferenceAmt());
					BigDecimal overUnder = paySelectionLine.getOpenAmt().subtract(paySelectionLine.getPayAmt())
							.subtract(paySelectionLine.getDiscountAmt()).subtract(paySelectionLine.getDifferenceAmt());
					payment.setOverUnderAmt(overUnder);
				} else {
					payment.setDiscountAmt(Env.ZERO);
				}
			}
			payment.setWriteOffAmt(Env.ZERO);
			payment.saveEx();
			//	
			paySelectionCheck.setC_Payment_ID (payment.getC_Payment_ID());
			paySelectionCheck.saveEx();	//	Payment process needs it
			//	Should start WF
			payment.processIt(DocAction.ACTION_Complete);
			payment.saveEx();
			if (payment.getTenderType().equals(MPayment.TENDERTYPE_Check))
				createBankaccountDocCheck(payment);
		}	//	new Payment

		//	Get Check Document No
		try
		{
			int no = Integer.parseInt(paySelectionCheck.getDocumentNo());
			if (lastDocumentNo.get() < no)
				lastDocumentNo.set(no);
		}
		catch (NumberFormatException ex)
		{
			log.config( "DocumentNo=" + paySelectionCheck.getDocumentNo());
		}
		paySelectionCheck.setIsPrinted(true);
		paySelectionCheck.setProcessed(true);
		paySelectionCheck.saveEx();

		log.config("Last Document No = " + lastDocumentNo.get());
		return null;	
	}
	
	private Boolean createBankaccountDocCheck(MPayment payment) {
		X_C_BankAccountDoc_Check bankAccountDoc_Checks = new X_C_BankAccountDoc_Check(getCtx(), 0, get_TrxName());
		X_C_BankAccountDoc bankAccountDoc = new X_C_BankAccountDoc(getCtx(), getBankAccountDocId(),
				get_TrxName());
		Integer currentNext = bankAccountDoc.getCurrentNext();
		bankAccountDoc_Checks.setC_BankAccountDoc_ID(getBankAccountDocId());
		bankAccountDoc_Checks.setC_Payment_ID(payment.getC_Payment_ID());
		bankAccountDoc_Checks.setAD_User_ID(payment.getCreatedBy());
		bankAccountDoc_Checks.setPayAmt(payment.getPayAmt());
		bankAccountDoc_Checks.setCheckNo(currentNext.toString());
		bankAccountDoc_Checks.saveEx();
		payment.setCheckNo(currentNext.toString());
		payment.set_CustomColumn("SerNo", bankAccountDoc.get_Value("SerNo"));		
		payment.saveEx();
		return true;
	}
	
	private BigDecimal C_PayselectionCheck_calculateWithHolding(int C_PayselectionCheck_ID){
		String sqltable =  "select coalesce(ad_Table_ID, -1) from ad_table where (tablename) = ?";
		 int ad_Table_ID= DB.getSQLValueEx(get_TrxName(), sqltable, "LCO_InvoiceWithholding");
		if (ad_Table_ID ==  -1)		 
				return Env.ZERO;
		String sql = "SELECT COALESCE (SUM (TaxAmt), 0) " +
		" FROM LCO_InvoiceWithholding " +
		" WHERE C_Invoice_ID in (select c_Invoice_ID from c_Payselectionline psl " +
		" 					  where c_Payselectioncheck_ID=?) AND  " +
		" IsActive = 'Y' AND " + 
		" IsCalcOnPayment = 'Y' AND  " +
		" Processed = 'N' AND  " +
		" C_AllocationLine_ID IS NULL ";
		BigDecimal whAmt = DB.getSQLValueBDEx(get_TrxName(), sql, C_PayselectionCheck_ID);
		
		 return whAmt;
	 }

	
	
	

	
	
	
	



}
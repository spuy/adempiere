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
/** Generated Model - DO NOT CHANGE */
package org.adempiere.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;

/** Generated Model for C_BankAccountDoc_Check
 *  @author Adempiere (generated) 
 *  @version Release 3.9.3 - $Id$ */
public class X_C_BankAccountDoc_Check extends PO implements I_C_BankAccountDoc_Check, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20200901L;

    /** Standard Constructor */
    public X_C_BankAccountDoc_Check (Properties ctx, int C_BankAccountDoc_Check_ID, String trxName)
    {
      super (ctx, C_BankAccountDoc_Check_ID, trxName);
      /** if (C_BankAccountDoc_Check_ID == 0)
        {
			setC_BankAccountDoc_Check_ID (0);
			setisVoided (false);
// N
        } */
    }

    /** Load Constructor */
    public X_C_BankAccountDoc_Check (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 3 - Client - Org 
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuffer sb = new StringBuffer ("X_C_BankAccountDoc_Check[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_AD_User getAD_User() throws RuntimeException
    {
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_Name)
			.getPO(getAD_User_ID(), get_TrxName());	}

	/** Set User/Contact.
		@param AD_User_ID 
		User within the system - Internal or Business Partner Contact
	  */
	public void setAD_User_ID (int AD_User_ID)
	{
		if (AD_User_ID < 1) 
			set_Value (COLUMNNAME_AD_User_ID, null);
		else 
			set_Value (COLUMNNAME_AD_User_ID, Integer.valueOf(AD_User_ID));
	}

	/** Get User/Contact.
		@return User within the system - Internal or Business Partner Contact
	  */
	public int getAD_User_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_User_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set C_BankAccountDoc_Check ID.
		@param C_BankAccountDoc_Check_ID C_BankAccountDoc_Check ID	  */
	public void setC_BankAccountDoc_Check_ID (int C_BankAccountDoc_Check_ID)
	{
		if (C_BankAccountDoc_Check_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_C_BankAccountDoc_Check_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_BankAccountDoc_Check_ID, Integer.valueOf(C_BankAccountDoc_Check_ID));
	}

	/** Get C_BankAccountDoc_Check ID.
		@return C_BankAccountDoc_Check ID	  */
	public int getC_BankAccountDoc_Check_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BankAccountDoc_Check_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_BankAccountDoc getC_BankAccountDoc() throws RuntimeException
    {
		return (org.compiere.model.I_C_BankAccountDoc)MTable.get(getCtx(), org.compiere.model.I_C_BankAccountDoc.Table_Name)
			.getPO(getC_BankAccountDoc_ID(), get_TrxName());	}

	/** Set Bank Account Document.
		@param C_BankAccountDoc_ID 
		Checks, Transfers, etc.
	  */
	public void setC_BankAccountDoc_ID (int C_BankAccountDoc_ID)
	{
		if (C_BankAccountDoc_ID < 1) 
			set_Value (COLUMNNAME_C_BankAccountDoc_ID, null);
		else 
			set_Value (COLUMNNAME_C_BankAccountDoc_ID, Integer.valueOf(C_BankAccountDoc_ID));
	}

	/** Get Bank Account Document.
		@return Checks, Transfers, etc.
	  */
	public int getC_BankAccountDoc_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BankAccountDoc_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_Payment getC_Payment() throws RuntimeException
    {
		return (org.compiere.model.I_C_Payment)MTable.get(getCtx(), org.compiere.model.I_C_Payment.Table_Name)
			.getPO(getC_Payment_ID(), get_TrxName());	}

	/** Set Payment.
		@param C_Payment_ID 
		Payment identifier
	  */
	public void setC_Payment_ID (int C_Payment_ID)
	{
		if (C_Payment_ID < 1) 
			set_Value (COLUMNNAME_C_Payment_ID, null);
		else 
			set_Value (COLUMNNAME_C_Payment_ID, Integer.valueOf(C_Payment_ID));
	}

	/** Get Payment.
		@return Payment identifier
	  */
	public int getC_Payment_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Payment_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Check No.
		@param CheckNo 
		Check Number
	  */
	public void setCheckNo (String CheckNo)
	{
		set_Value (COLUMNNAME_CheckNo, CheckNo);
	}

	/** Get Check No.
		@return Check Number
	  */
	public String getCheckNo () 
	{
		return (String)get_Value(COLUMNNAME_CheckNo);
	}

	/** Set Transaction Date.
		@param DateTrx 
		Transaction Date
	  */
	public void setDateTrx (Timestamp DateTrx)
	{
		set_Value (COLUMNNAME_DateTrx, DateTrx);
	}

	/** Get Transaction Date.
		@return Transaction Date
	  */
	public Timestamp getDateTrx () 
	{
		return (Timestamp)get_Value(COLUMNNAME_DateTrx);
	}

	/** Set isVoided.
		@param isVoided isVoided	  */
	public void setisVoided (boolean isVoided)
	{
		set_Value (COLUMNNAME_isVoided, Boolean.valueOf(isVoided));
	}

	/** Get isVoided.
		@return isVoided	  */
	public boolean isVoided () 
	{
		Object oo = get_Value(COLUMNNAME_isVoided);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Payment amount.
		@param PayAmt 
		Amount being paid
	  */
	public void setPayAmt (BigDecimal PayAmt)
	{
		set_Value (COLUMNNAME_PayAmt, PayAmt);
	}

	/** Get Payment amount.
		@return Amount being paid
	  */
	public BigDecimal getPayAmt () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_PayAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Immutable Universally Unique Identifier.
		@param UUID 
		Immutable Universally Unique Identifier
	  */
	public void setUUID (String UUID)
	{
		set_Value (COLUMNNAME_UUID, UUID);
	}

	/** Get Immutable Universally Unique Identifier.
		@return Immutable Universally Unique Identifier
	  */
	public String getUUID () 
	{
		return (String)get_Value(COLUMNNAME_UUID);
	}
}
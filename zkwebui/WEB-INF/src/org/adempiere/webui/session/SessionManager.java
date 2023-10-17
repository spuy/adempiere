/******************************************************************************
 * Product: Posterita Ajax UI 												  *
 * Copyright (C) 2007 Posterita Ltd.  All Rights Reserved.                    *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Posterita Ltd., 3, Draper Avenue, Quatre Bornes, Mauritius                 *
 * or via info@posterita.org or http://www.posterita.org/                     *
 *****************************************************************************/

package org.adempiere.webui.session;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.IWebClient;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.desktop.IDesktop;
import org.adempiere.webui.util.UserPreference;
import org.compiere.model.MUser;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.SecureEngine;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.impl.ExecutionCarryOver;

import javax.servlet.http.HttpSession;

import static org.adempiere.webui.session.SessionContextListener.SERVLET_SESSION_ID;

/**
 * 
 * @author <a href="mailto:agramdass@gmail.com">Ashley G Ramdass</a>
 * @date Feb 25, 2007
 * @version $Revision: 0.10 $
 */
public class SessionManager
{
    public static final String SESSION_APPLICATION = "SessionApplication";
    
    private static CLogger log = CLogger.getCLogger(SessionManager.class);
	private static final Map<String, WeakReference<IWebClient>> applicationCache = Collections.synchronizedMap(new Hashtable<>());
	private static final Map<String, WeakReference<IDesktop>> desktopCache = Collections.synchronizedMap(new Hashtable<>());
	private static final Map<String, HttpSession> sessionCache = Collections.synchronizedMap(new Hashtable<>());
	private static final Map<String, Properties> sessionContextCache = Collections.synchronizedMap(new Hashtable<>());
	private static final Map<String, ExecutionCarryOver> executionCarryOverCache = Collections.synchronizedMap(new Hashtable<>());
	private static final Map<String, UserPreference> sessionUserPreferenceCache = Collections.synchronizedMap(new Hashtable<>());
	private static final Map<String, String> userAuthenticationCache = Collections.synchronizedMap(new Hashtable<>());

    public static boolean isUserLoggedIn(Properties ctx)
    {
        String adUserId = Env.getContext(ctx, "#AD_User_ID");
        String adRoleId = Env.getContext(ctx, "#AD_Role_ID");
        String adClientId = Env.getContext(ctx, "#AD_Client_ID");
        String adOrgId = Env.getContext(ctx, "#AD_Org_ID");

        return (!"".equals(adUserId) && !"".equals(adRoleId)
                && !"".equals(adClientId) && !"".equals(adOrgId));
    }
    
    private static Session getSession()
    {
        return  Executions.getCurrent().getDesktop().getSession();
    }
    
    public static void setSessionApplication(IWebClient app)
    {
    	Desktop desktop = AEnv.getDesktop();
		if (desktop != null) {
			desktop.setAttribute(SESSION_APPLICATION, new WeakReference<IWebClient>(app));
		} else {
			log.severe("Unable to save session application on desktop");
		}
    }
    
    public static IDesktop getAppDesktop()
    {
    	if (getSessionApplication() == null) {
    		throw new AdempiereException("Can't get access to desktop");
    	}
    	return getSessionApplication().getAppDeskop();
    }
    
    @SuppressWarnings("unchecked")
	public static IDesktop getAppDesktop(Desktop desktop)
    {
		IWebClient app = null;
		WeakReference<IWebClient> wref = (WeakReference<IWebClient>) desktop.getAttribute(SESSION_APPLICATION);
		app = wref != null ? wref.get() : null;
		
		if (app == null) {
			wref = (WeakReference<IWebClient>) AEnv.getDesktop().getAttribute(SESSION_APPLICATION);
			app = wref != null ? wref.get() : null;
			if (app != null) {
				log.warning("Unable to access desktop, used default instead.");
			}
		}
		if (app == null) {
			log.severe("Still can't access desktop. Must be investigated.");
		}
		return app.getAppDeskop();
    }
    
    public static IWebClient getSessionApplication()
    {
    	Desktop desktop = AEnv.getDesktop();
		IWebClient app = null;
		if (desktop != null)
		{
			@SuppressWarnings("unchecked")
			WeakReference<IWebClient> wref = (WeakReference<IWebClient>) desktop.getAttribute(SESSION_APPLICATION);
			app = wref != null ? wref.get() : null;
		}
        return app;
    }
    
    public static void clearSession()
    {
        Env.getCtx().clear();
        Session session = getSession();
        session.removeAttribute(SessionContextListener.SESSION_CTX);
        session.invalidate();
    }
    
    public static void logoutSession()
    {
        getSessionApplication().logout();
    }

	public static void changeRole(MUser user)
	{
		IWebClient app = getSessionApplication();
		if (app != null)
			app.changeRole(user);
	}
	
	public static void logoutSessionAfterBrowserDestroyed()
	{
		IWebClient app = getSessionApplication();
		if (app != null)
			app.logoutAfterTabDestroyed();
	}
	
    public static boolean activateDesktop(Desktop desktop)
     {
 		if (Events.inEventListener()) {
 			return true;
 		}
    	if (desktop ==  null) {
    		log.severe("Attempted to activate NULL desktop.");
    		return false;
    	}
     	
 		try {
 			if (Executions.activate((org.zkoss.zk.ui.Desktop) desktop, 500)) {
 				return true;
 			} else {
 				log.fine("Unable to grab control of desktop.");
 				if (!desktop.isServerPushEnabled()) {
 					log.severe("Unexpected. Server Push not enabled");
 				}
 				
 			}
 			
 		} catch (Exception e) {
			log.severe(e.getMessage());
 		} finally {
 		}
 		return false;
     }

    public static void releaseDesktop(Desktop desktop)
	{
		if (desktop  == null) {
			return;
		}
		Executions.deactivate((org.zkoss.zk.ui.Desktop) desktop);
//		if (Env.getContext(Env.getCtx(), ZK_DESKTOP_ISACTIVE).equals("Y")) {
//			Env.setContext(Env.getCtx(), ZK_DESKTOP_ISACTIVE, "N");
//		}
		
	}


	public static void setUserAuthentication(String sessionId, String authentication) {
		userAuthenticationCache.put(sessionId, SecureEngine.encrypt(authentication));
	}

	public static String getUserAuthentication(String sessionId) {
		return SecureEngine.decrypt(userAuthenticationCache.get(sessionId));
	}

	public static void removeUserAuthentication(String sessionId) {
		Optional.ofNullable(userAuthenticationCache.get(sessionId))
				.ifPresent(userAuthentication -> {
					userAuthenticationCache.remove(sessionId);
					userAuthentication = null;
				});
	}
	public static Properties getSessionContext(String sessionId) {
		return sessionContextCache.get(sessionId);
	}

	public static boolean containsKeySessionContext(String sessionId) {
		return sessionContextCache.containsKey(sessionId);
	}

	public static void removeSessionContext(String sessionId) {
		Optional.ofNullable(sessionContextCache.get(sessionId))
				.ifPresent(context -> {
					sessionContextCache.remove(sessionId);
					context = null;
				});
	}

	public static Map<String, Properties> getSessionContextCache() {
		return sessionContextCache;
	}

	public static void setExecutionCarryOverCache(String sessionId, ExecutionCarryOver executionCarryOver) {
		executionCarryOverCache.put(sessionId,executionCarryOver);
	}

	public static ExecutionCarryOver getExecutionCarryOver(String sessionId) {
		return executionCarryOverCache.get(sessionId);
	}

	public static boolean existsExecutionCarryOver(String sessionId) {
		return executionCarryOverCache.containsKey(sessionId);
	}
	public static void loadUserPreference(Integer authenticatedUserId) {
		String sessionId = Env.getContext(Env.getCtx(), SERVLET_SESSION_ID);
		UserPreference userPreference = new UserPreference();
		userPreference.loadPreference(authenticatedUserId);
		sessionUserPreferenceCache.put(sessionId, userPreference);
	}

	public static UserPreference getUserPreference() {
		String sessionId = Env.getContext(Env.getCtx(), SERVLET_SESSION_ID);
		return getUserPreference(sessionId);
	}

	public static UserPreference getUserPreference(String sessionId) {
		return sessionUserPreferenceCache.get(sessionId);
	}

	public static void removeSessionUserPreference(String sessionId) {
		Optional.ofNullable(sessionUserPreferenceCache.get(sessionId))
				.ifPresent(
						userPreference -> {
							// Then
							sessionUserPreferenceCache.remove(sessionId);
							userPreference = null;
						}

				);
	}
	public static void setApplication(String sessionId, IWebClient application) {
		applicationCache.put(sessionId, new WeakReference<>(application));
	}
}

package ru.rlisystems.docviewer;

import lombok.extern.java.Log;

import javax.enterprise.inject.Produces;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Synchronization;
import javax.transaction.TransactionManager;
import java.lang.reflect.Method;
import java.util.logging.Level;

@Log
public class TransactionUtils
{
	private static String[][] JNDI_CATALOG = {
			{ "java:jboss/TransactionManager", "JBoss AS 7" },
			{ "java:/TransactionManager", "JBoss AS 4 ~ 6, JRun4" },
			{ "java:comp/TransactionManager", "Resin 3.x" },
			{ "java:appserver/TransactionManager", "Sun Glassfish" },
			{ "java:pm/TransactionManager", "Borland, Sun" },
			{ "javax.transaction.TransactionManager", "BEA WebLogic" },
			{ "java:comp/UserTransaction", "Resin, Orion, JOnAS (JOTM)" }
	};

	private static final String[][] FACTORY_CATALOG = {
			{ "com.ibm.ws.Transaction.TransactionManagerFactory", "IBM WebSphere 5.1" },
			{ "com.ibm.ejs.jts.jta.TransactionManagerFactory", "IBM WebSphere 5.0" },
			{ "com.ibm.ejs.jts.jta.JTSXA", "IBM WebSphere 4" }
	};


	@Produces
	public static TransactionManager getTransactionManager ()
	{
		return getTransactionManager(Thread.currentThread().getContextClassLoader());
	}

	public static TransactionManager getTransactionManager (ClassLoader classLoader)
	{
		try {
			InitialContext initialContext = new InitialContext();
			try {
				for (String[] jndiTuple : JNDI_CATALOG) {
					Object jndiObject;
					try {
						jndiObject = initialContext.lookup(jndiTuple[0]);
					}
					catch (NamingException ex) {
						continue;
					}
					if (jndiObject instanceof TransactionManager) {
						log.finest("Используется " + jndiTuple[1] + " TransactionManager (jndi " + jndiTuple[0] + ")");
						return (TransactionManager) jndiObject;
					}
				}
			}
			finally {
				initialContext.close();
			}
		}
		catch (NamingException ex) {
			throw new RuntimeException("Ошибка при работе с JNDI каталогом", ex);
		}

		for (String[] factoryTuple : FACTORY_CATALOG) {
			try {
				Class<?> type = classLoader.loadClass(factoryTuple[0]);
				log.finest("Найден " + factoryTuple[1] + " TransactionManager (factory " + factoryTuple[0] + ")");
				Method method = type.getMethod("getTransactionManager");
				return (TransactionManager) method.invoke(null);
			}
			catch (ClassNotFoundException ex)
			{ }
			catch (Exception ex) {
				log.log(Level.WARNING, "Невозможно получить " + factoryTuple[1] + " TransactionManager (" +
						factoryTuple[0] + ".getTransactionManager()", ex);
			}
		}
		throw new RuntimeException("Не найден ни один из известных TransactionManager");
	}

	@FunctionalInterface
	public static interface BeforeCompletionSynchronization extends Synchronization
	{
		@Override
		default void afterCompletion (int status)
		{ };
	}

	@FunctionalInterface
	public static interface AfterCompletionSynchronization extends Synchronization
	{
		@Override
		default void beforeCompletion ()
		{ };
	}
}

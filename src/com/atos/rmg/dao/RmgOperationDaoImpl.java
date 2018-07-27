package com.atos.rmg.dao; 

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;

import com.atos.rmg.beans.CountryPractiseBean;
import com.atos.rmg.beans.UserBean;
import com.atos.rmg.entity.Country;
import com.atos.rmg.entity.CountryPractise;
import com.atos.rmg.entity.DemandDetails;
import com.atos.rmg.entity.Language;
import com.atos.rmg.entity.Manager;
import com.atos.rmg.entity.Personalise;
import com.atos.rmg.entity.SubcoApprovalTransLog;
import com.atos.rmg.entity.TransactionLog;
import com.atos.rmg.entity.User;

/**
 * 
 * @author A180562
 *
 */

public class RmgOperationDaoImpl implements RmgOperationDao {

	// private static SessionFactory sessionFactory = new
	// Configuration().configure().buildSessionFactory();
	@Autowired
	private SessionFactory sessionFactory;
	private User user;
	private int SubConSeqId = 0;
	final static Logger log = Logger.getLogger(RmgOperationDaoImpl.class.getName());

	/**
	 * This method saves manager in DB.
	 * 
	 * @param manager
	 * @throws Exception
	 * @return void
	 */
	@Override
	public void save(Manager manager) throws Exception {
		Transaction transaction = null;
		Session session = sessionFactory.openSession();
		try {

			transaction = session.beginTransaction();
			session.save(manager);
			transaction.commit();
		} catch (Exception e) {
			if (transaction != null)
				transaction.rollback();
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
	}

	/**
	 * This method used to receive manager details from database on basis of
	 * search criteria.
	 * 
	 * @param searchCritera
	 * @throws Exception
	 * @return List<Manager>
	 */
	@Override
	public List<Manager> search(String searchCritera) throws Exception {
		List<Manager> listOfManager = null;
		Session session = sessionFactory.openSession();
		try {

			if (searchCritera != null && !searchCritera.equals("")) {
				listOfManager = session.createQuery("from Manager where employeeId = '" + searchCritera + "'").list();
			} else {
				listOfManager = session.createQuery("from Manager").list();
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return listOfManager;
	}

	/**
	 * This method used to validate user after login.
	 * 
	 * @param user
	 * @throws Exception
	 * @return boolean
	 */
	@Override
	public boolean validateUser(User user) throws Exception {
		List<User> listOfUser = null;
		Session session = sessionFactory.openSession();
		try {
			String role = user.getUserWorkflowRole();
			Query query = session.createQuery(
					" from User u where u.userID = :userName and u.userPwd = :passWord and u.userWorkflowRole=:role");
			query.setParameter("userName", user.getUserID());
			query.setParameter("passWord", user.getUserPwd());
			query.setParameter("role", role);
			query.setCacheable(true);
			query.setCacheRegion("validateUser");
			listOfUser = query.list();
			if (listOfUser != null && listOfUser.size() == 1) {
				setUser(listOfUser.get(0));
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return false;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public User getUser() {
		return user;
	}

	/**
	 * This method used to set update password for respective user in Database.
	 * 
	 * @param user
	 * @throws Exception
	 * @return boolean
	 */
	public boolean forgetPswd(User user) throws Exception {
		Session session = sessionFactory.openSession();
		Transaction tx = null;
		int status = 0;
		try {
			tx = session.beginTransaction();
			Query q = session.createQuery("update User set UserPwd=:userPwd where UserID=:userID");
			q.setParameter("userPwd", user.getUserPwd());
			q.setParameter("userID", user.getUserID());
			q.setCacheable(true);
			q.setCacheRegion("forgetPswd");
			status = q.executeUpdate();
			log.info("status" + status);
			tx.commit();

			if (status == 1) {
				return true;
			} else {
				return false;
			}

		} catch (Exception e) {
			if (tx != null)
				tx.rollback();
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return false;
	}

	/**
	 * This method used to verify user's availability from database.
	 * 
	 * @param user
	 * @throws Exception
	 * @return boolean
	 */
	public boolean UserDetails(User user) throws Exception {
		List<User> listOfUser = null;
		Session session = sessionFactory.openSession();
		try {
			Query query = session.createQuery(" from User u where u.userID = :userID");
			log.info("UserID --" + user.getUserID());
			query.setParameter("userID", user.getUserID());
			query.setCacheable(true);
			query.setCacheRegion("UserDetails");
			listOfUser = query.list();
			if (listOfUser.size() == 1) {
				setUser(listOfUser.get(0));
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return false;
	}
	/*
	 * unused methods public void addCountryDetails(Country country){ try{
	 * Session session = sessionFactory.openSession(); session.save(country);
	 * 
	 * }catch(Exception e){ log.error(e.getMessage()); } }
	 * 
	 * public void addCountryPracticeDetails(CountryPractise countryPractise){
	 * try{ Session session = sessionFactory.openSession();
	 * session.save(countryPractise);
	 * 
	 * }catch(Exception e){ log.error(e.getMessage()); } }
	 */

	/**
	 * This method used to make an entry of newly created input request as well
	 * as update of any existing input requests into transaction log table.
	 * 
	 * @param transactionLog
	 * @param demand_ID
	 * @param gbu
	 * @param country
	 * @param globalPractise
	 * @param subPractise
	 * @param userId
	 * @return int
	 */
	public String addTransactionLogs(TransactionLog transactionLog, int demand_ID, String gbu, String country,
			String globalPractise, String subPractise, String userId) {
		Transaction transaction = null;
		Session session = sessionFactory.openSession();
		String returnValue = "";
		try {
			String countryValue = "";
			String globalPractiseV = "";
			String subPractiseV = "";
			if (country.contains(":")) {
				countryValue = country.split(":")[1];
			} else {
				countryValue = country;
			}
			if (globalPractise.contains(":")) {
				globalPractiseV = globalPractise.split(":")[1];
			} else {
				globalPractiseV = globalPractise;
			}

			if (globalPractise.contains(":")) {
				subPractiseV = subPractise.split(":")[1];
			} else {
				subPractiseV = subPractise;
			}
			String marginV = "";
			if (transactionLog.getMargin().contains("%")) {
				marginV = transactionLog.getMargin().split("%")[0];
			} else {
				marginV = transactionLog.getMargin();
			}
			Double margin = Double.parseDouble(marginV);
			transactionLog.setMargin(margin.toString());

			// Double avgDailyCost =
			// Double.parseDouble(transactionLog.getAvgDailyCost());
			// transactionLog.setAvgDailyCost(avgDailyCost.toString());

			transaction = session.beginTransaction();
			Query query = session.createQuery(" from Country c where c.gbuName = :gbu and c.countryName = :country");
			query.setParameter("gbu", gbu);
			query.setParameter("country", countryValue);
			query.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
			query.setCacheable(true);
			query.setCacheRegion("addTransactionLogs");
			List<Country> countryList = query.list();
			transactionLog.setgBUID(countryList.get(0).getGbuID().toString());
			transactionLog.setCountryID(countryList.get(0).getCountryID().toString());
			transactionLog.setAssignedCountryID(countryList.get(0).getCountryID());
			transactionLog.setAssignedLocation(transactionLog.getAssignedLocation());

			Query queryF = session.createQuery("select pm from Country cm,CountryPractise pm "
					+ " where cm.countryID=pm.countryID and cm.gbuID =:gbuID "
					+ "and cm.countryID=:countryID and pm.gPracticeName=:gPracticeName "
					+ "and pm.subPractiseName=:subPractiseName");
			queryF.setParameter("countryID", countryList.get(0).getCountryID());
			queryF.setParameter("gbuID", countryList.get(0).getGbuID().toString());
			queryF.setParameter("gPracticeName", globalPractiseV);
			queryF.setParameter("subPractiseName", subPractiseV);
			queryF.setCacheable(true);
			queryF.setCacheRegion("addTransactionLogs1");
			List<CountryPractise> countryPListF = queryF.list();
			transactionLog.setgPracticeID(countryPListF.get(0).getgPracticeID());
			transactionLog.setSubPracticeID(countryPListF.get(0).getSubPracticeID());

			if (demand_ID != 0) {
				transactionLog.setSubcoDemandId(demand_ID);
				log.info("inside if loop and demand is not null---------" + demand_ID);
				returnValue = "U" + demand_ID;
			} else {
				Query queryTh1 = session.createSQLQuery(" select next value for SubCo_DemandID_Seq ");
				int key = (int) ((BigInteger) queryTh1.uniqueResult()).longValue();
				demand_ID = key;
				returnValue = "N" + demand_ID;
				log.info("Auto created SubConSeqId = " + demand_ID);
				transactionLog.setSubcoDemandId(demand_ID);
			}
			session.saveOrUpdate(transactionLog);
			transaction.commit();
		} catch (Exception e) {
			if (transaction != null)
				transaction.rollback();
			log.error(e.getMessage());
			log.error("Insert failed in transactionLog : " + returnValue);
			returnValue = "insertFailed";
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return returnValue;
	}

	/**
	 * This method used to make an entry of manager details of newly created
	 * input request as well as any update of manager details of existing input
	 * requests into Manager master table.
	 * 
	 * @param manager
	 * @param demand_ID
	 * @param gbuName
	 * @param userId
	 * @throws Exception
	 * @return void
	 */
	@Override
	public void addManager(Manager manager, int demand_ID, String gbuName, String userId) throws Exception {
		Transaction transaction = null;
		Session session = sessionFactory.openSession();
		try {
			transaction = session.beginTransaction();
			manager.setSubcoDemandID(demand_ID);
			/*
			 * if (demand_ID != 0) { manager.setSubcoDemandID(demand_ID); } else
			 * { manager.setSubcoDemandID(SubConSeqId); }
			 */
			session.saveOrUpdate(manager);
			transaction.commit();
		} catch (Exception e) {
			if (transaction != null)
				transaction.rollback();
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
	}

	/**
	 * This method used to get Demand status on basis on Demand status ID from
	 * database.
	 * 
	 * @param demandStatus
	 * @return List<DemandDetails>
	 */
	@SuppressWarnings("unchecked")
	public List<DemandDetails> getDemandStatusID(String demandStatus) {
		List<DemandDetails> listOfDemandDetails = null;
		Session session = sessionFactory.openSession();
		try {
			Query query = session.createQuery("select dd from DemandDetails dd where demandStatusID=:demandStatus");
			query.setParameter("demandStatus", demandStatus);
			query.setCacheable(true);
			query.setCacheRegion("getDemandStatusID");
			listOfDemandDetails = query.list();

		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return listOfDemandDetails;
	}

	/**
	 * This method used to get list of languages from Database.
	 * 
	 * @return List<Language>
	 */
	public List<Language> getLanguages() {
		List<Language> listOfLanguage = null;
		Session session = sessionFactory.openSession();
		try {
			Query query = session.createQuery(" from Language");
			query.setCacheable(true);
			query.setCacheRegion("getLanguages");
			listOfLanguage = query.list();

		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return listOfLanguage;
	}

	/**
	 * This method get list of countries from database.
	 * 
	 * @return List<Country>
	 */
	@SuppressWarnings("unchecked")
	public List<Country> getCountries() {
		List<Country> listOfCountry = null;
		Session session = sessionFactory.openSession();
		try {
			Query query = session.createQuery("from Country");
			query.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
			query.setCacheable(true);
			query.setCacheRegion("country");
			listOfCountry = query.list();

		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return listOfCountry;
	}

	/**
	 * This method receives list of countriesPractice respective to SubcoDemand
	 * ID.
	 * 
	 * @param subcoDemandID
	 * @return List<Country>
	 */
	public List<Country> getCountryInitialValue(int subcoDemandID) {
		List<TransactionLog> listOfTransactionLog = null;
		List<Country> countryRecord = null;
		Session session = sessionFactory.openSession();
		try {
			Query query = session
					.createQuery("select tl from TransactionLog tl " + "where tl.subcoDemandId=:subcoDemandID");
			query.setParameter("subcoDemandID", subcoDemandID);
			query.setCacheable(true);
			query.setCacheRegion("getCountryInitialValue");
			listOfTransactionLog = query.list();

			TransactionLog transactionLog = listOfTransactionLog.get(0);
			String gbuID = transactionLog.getgBUID();
			String countryID = transactionLog.getCountryID();
			Query queryCountry = session
					.createQuery("select c from Country c where c.gbuID=:gbuID " + "and c.countryID=:countryID");
			queryCountry.setParameter("gbuID", gbuID);
			queryCountry.setParameter("countryID", countryID);
			queryCountry.setCacheable(true);
			queryCountry.setCacheRegion("getCountryInitialValue1");
			countryRecord = queryCountry.list();

		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return countryRecord;
	}

	/**
	 * This method receives list of countries respective to SubcoDemand ID.
	 * 
	 * @param subcoDemandID
	 * @return List<CountryPractise>
	 */
	public List<CountryPractise> getCountryPractiseInitialValue(int subcoDemandID) {
		List<TransactionLog> listOfTransactionLog = null;
		List<CountryPractise> countryPractiseRecord = null;
		Session session = sessionFactory.openSession();
		try {
			Query query = session
					.createQuery("select tl from TransactionLog tl " + "where tl.subcoDemandId=:subcoDemandID");
			query.setParameter("subcoDemandID", subcoDemandID);
			query.setCacheable(true);
			query.setCacheRegion("getCountryPractiseInitialValue");
			listOfTransactionLog = query.list();

			TransactionLog transactionLog = listOfTransactionLog.get(0);
			String countryID = transactionLog.getCountryID();
			String gPracticeID = transactionLog.getgPracticeID();
			String subPracticeID = transactionLog.getSubPracticeID();

			Query queryCountryPratice = session.createQuery(
					"select c from CountryPractise c " + "where c.countryID=:countryID and c.gPracticeID=:gPracticeID "
							+ "and c.subPracticeID=:subPracticeID");
			queryCountryPratice.setParameter("countryID", countryID);
			queryCountryPratice.setParameter("gPracticeID", gPracticeID);
			queryCountryPratice.setParameter("subPracticeID", subPracticeID);
			queryCountryPratice.setCacheable(true);
			queryCountryPratice.setCacheRegion("getCountryPractiseInitialValue1");
			countryPractiseRecord = queryCountryPratice.list();

		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return countryPractiseRecord;
	}

	/**
	 * This method returns list of countries on basis of user logged in.
	 * 
	 * @param userBean
	 * @return List<Country>
	 */
	@SuppressWarnings("unchecked")
	public List<Country> getCountries(UserBean userBean) {
		List<Country> listOfCountry = null;
		Session session = sessionFactory.openSession();
		try {

			Query query = session.createQuery(" select c from Country c where c.gbuID in "
					+ "(select um.gbuId from User um where um.userID= :userID) AND c.countryID in "
					+ "(select um.countryID from User um where um.userID= :userID)");

			query.setParameter("userID", userBean.getUserID());
			// query.setCacheable(true);
			// query.setCacheRegion("getCountries");
			listOfCountry = query.list();

		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return listOfCountry;
	}

	/**
	 * This method returns list of countries on basis of selected GBU from
	 * database.
	 * 
	 * @param gbuvalue
	 * @return List<String>
	 */
	@SuppressWarnings("unchecked")
	public List<String> getCountriesForGBU(String gbuvalue) {
		List<String> listOfCountryForGBU = new ArrayList<String>();
		Session session = sessionFactory.openSession();
		try {
			Query query = session.createQuery(" from Country");
			listOfCountryForGBU = (List<String>) session
					.createQuery("select distinct(countryName) " + " from Country where gbuName=:gbu")
					.setParameter("gbu", gbuvalue).setCacheable(true).list();

		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return listOfCountryForGBU;
	}

	/**
	 * This method returns list of global Practice details on basis of selected
	 * country from database.
	 * 
	 * @param country
	 * @return List<String>
	 */
/*	--------------*/
	@SuppressWarnings("unchecked")
	public List<String> getGPracticesForCountry(String country) {
		List<String> listOfCountryForGBU = null;
		Session session = sessionFactory.openSession();
		try {
			Query query = session.createQuery("select distinct(cpm.gPracticeName) "
					+ "from CountryPractise cpm, Country cm where cm.countryID=cpm.countryID "
					+ "and cpm.countryID IN (select cc.countryID from Country cc " + "where cc.countryName=:country)")
					.setParameter("country", country);
			query.setCacheable(true);
			query.setCacheRegion("getGPracticesForCountry");
			listOfCountryForGBU = query.list();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return listOfCountryForGBU;
	}

	/**
	 * This method returns list of country practice.
	 * 
	 * @return List<CountryPractise>
	 */
	@SuppressWarnings("unchecked")
	public List<String> getGPraticenamePRM(UserBean userBean) {
		List<String> listOfCountryPractise = null;
		Session session = sessionFactory.openSession();
		try {
			Query query = session.createQuery("select distinct(cpm.gPracticeName) "
					+ "from CountryPractise cpm, Country cm where cm.countryID=cpm.countryID "
					+ "and cpm.countryID IN "
					+ "(select um.countryID from User um where um.userID= :userID "
						+ "and um.userWorkflowRole= :userRole) ");
					//+ "and um.userWorkflowRole= :userRole) ");
					
			query.setParameter("userID", userBean.getUserID());
			query.setParameter("userRole", userBean.getUserWorkflowRole());
			query.setCacheable(true);
			query.setCacheRegion("getCountriesPractiseforPRM");
			listOfCountryPractise = query.list();

		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}

		return listOfCountryPractise;
	}

	
	/**
	 * This method returns list of country practice.
	 * 
	 * @return List<CountryPractise>
	 */
	@SuppressWarnings("unchecked")
	public List<String> getGPraticenameForPRM(String country) {
		List<String> listOfCountryPractise = null;
		Session session = sessionFactory.openSession();
		try {
			Query query = session.createQuery("select distinct(cpm.gPracticeName) "
					+ "from CountryPractise cpm, Country cm where cm.countryID=cpm.countryID "
					+ "and cpm.countryID IN "
					+ "(select c.countryID from Country c where c.countryName= :countryValue ) ");
					//+ "and um.userWorkflowRole= :userRole) ");
			
			query.setParameter("countryValue", country);		
			query.setCacheable(true);
			query.setCacheRegion("getCountriesPractiseforPRM");
			listOfCountryPractise = query.list();

		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}

		return listOfCountryPractise;
	}

	
	
	/**
	 * This method returns list of country practice details on basis of user logged in.
	 * @return List<CountryPractise>
	 */
	@SuppressWarnings("unchecked")
	public List<CountryPractise> getCountriesPractise(UserBean userBean){
		List<CountryPractise> listOfCountryPractise = null;
		Session session = sessionFactory.openSession();
		try{
			if(userBean!=null && ( userBean.getUserWorkflowRole().trim().equals("PM") 
					|| userBean.getUserWorkflowRole().trim().equals("PRM")
					|| userBean.getUserWorkflowRole().trim().equals("GPRM"))) {
				
				
				Query query = session.createQuery(" select distinct cp from CountryPractise cp "
						+ "where cp.gPracticeID in (select um.gPracticeID from "
						+ "User um where um.userID= :userID and um.userWorkflowRole= :userRole) AND cp.countryID in "
						+ "(select um.countryID from User um where um.userID= :userID "
						+ "and um.userWorkflowRole= :userRole) "
						+ "AND cp.subPracticeID in (select um.subPracticeID from User um "
						+ "where um.userID= :userID and um.userWorkflowRole= :userRole)");
				query.setParameter("userID", userBean.getUserID());
				query.setParameter("userRole", userBean.getUserWorkflowRole());
				query.setCacheable(true);
				query.setCacheRegion("getCountriesPractise");
				listOfCountryPractise = query.list();
				
			}
		}catch(

	Exception e)
	{
			log.error(e.getMessage());
		}finally
	{
			
			
		if (session != null && session.isOpen())
			session.close();
	}
	// System.out.println("listOfCountryPractise DAO IMP=
	// "+listOfCountryPractise.size());
	return listOfCountryPractise;
	}

	/**
	 * This method 
	 * @param gPractiseName
	 * @param countryName
	 * @param gbuName
	 * @return List<String>
	 */
	@SuppressWarnings("unchecked")
	public List<String> getCountrySubPractices(String gPractiseName, String countryName,String gbuName){
		List<String> listOfCountryPractise = null;
		Session session = sessionFactory.openSession();
		try{
			Query query = session.createQuery("select distinct(cp.subPractiseName) from "
					+ "CountryPractise cp where cp.gPracticeName=:gPractiseName "
					+ "and cp.countryID IN (select cc.countryID from Country cc "
					+ "where cc.countryName=:country and cc.gbuName=:gbu)");
			query.setParameter("gPractiseName", gPractiseName);
			query.setParameter("country", countryName);
			query.setParameter("gbu", gbuName);
			query.setCacheable(true);
			query.setCacheRegion("getCountrySubPractices");
			listOfCountryPractise = query.list();	
				
		}catch(Exception e){
			log.error(e.getMessage());
		}finally {
			if(session!=null && session.isOpen()) session.close();
		 }
		
		return listOfCountryPractise;
	}

	/**
	 * This method gets list of manager details from DB.
	 * @throws Exception
	 * @return List<Manager>
	 */
	
	@Override
	public List<Manager> findAll() throws Exception {
		List<Manager> listOfManager = null;
		Session session = sessionFactory.openSession();
		try{
			listOfManager = session.createQuery("from Manager").list();
				
		}catch(Exception e){
			log.error(e.getMessage());
		}finally {
			if(session!=null && session.isOpen()) session.close();
		 }
		return listOfManager;
	}

	/**
	 * This method used to receive transaction log details for respective
	 * SubcoDemandId
	 * 
	 * @param subcoDemandID
	 * @return List<TransactionLog>
	 */
	public List<TransactionLog> getTransactionLogForRecord(int subcoDemandID) {
		List<TransactionLog> listOfTransactionLog = null;
		Session session = sessionFactory.openSession();
		try {
			Query query = session
					.createQuery("select tl from TransactionLog tl " + "where tl.subcoDemandId=:subcoDemandID");
			query.setParameter("subcoDemandID", subcoDemandID);
			query.setCacheable(true);
			query.setCacheRegion("getTransactionLogForRecord");
			listOfTransactionLog = query.list();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return listOfTransactionLog;
	}

	/**
	 * This method used to receive manager details for respective SubcoDemandId
	 * 
	 * @param subcoDemandID
	 * @return List<Manager>
	 */
	public List<Manager> getManagerDetailsForRecord(int subcoDemandID) {
		List<Manager> listOfManagerDetails = null;
		Session session = sessionFactory.openSession();
		try {
			Query query = session.createQuery("select m from Manager m " + "where m.subcoDemandID=:subcoDemandID");
			query.setParameter("subcoDemandID", subcoDemandID);
			query.setCacheable(true);
			query.setCacheRegion("getManagerDetailsForRecord");
			listOfManagerDetails = query.list();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return listOfManagerDetails;
	}

	/**
	 * This method used to receive transaction log details for respective user
	 * logged in.
	 * 
	 * @param userBean
	 * @return List<TransactionLog>
	 */
	public List<TransactionLog> getTransactionLogs(UserBean userBean) {
		List<TransactionLog> listOfTransactionLog = null;
		Session session = sessionFactory.openSession();
		try {
			String statusID = "";
			String sql = "";
			if (userBean != null && userBean.getUserWorkflowRole().trim().equals("PM")) {

				//statusID = " tl.demandStatus in ('101','111','104', '115','118') ";
				statusID = " tl.demandStatus in ('101','104', '115') "; 
				sql = "select tl from TransactionLog tl where tl.gBUID in "
						+ "(select um.gbuId from User um where um.userID= :userID and um.userWorkflowRole='PM') "
						+ "AND tl.countryID in (select um.countryID from User um where um.userID= :userID "
						+ "and um.userWorkflowRole='PM') " + " AND " + statusID
						+ "and tl.gPracticeID in (select um.gPracticeID from User um where um.userID= :userID "
						+ "and um.userWorkflowRole='PM') " + " order by subcoDemandId desc";
			} else if (userBean != null && userBean.getUserWorkflowRole().trim().equals("PRM")) {
				//statusID = " tl.demandStatus in ('101','116','119') ";
				statusID = " tl.demandStatus in ('101','116') ";
				sql = "select tl from TransactionLog tl where tl.gBUID in "
						+ "(select um.gbuId from User um where um.userID= :userID and um.userWorkflowRole='PRM') "
						/*
						 * +
						 * "AND tl.gPracticeID in (select um.gPracticeID from User um where um.userID= :userID "
						 * + "and um.userWorkflowRole='PRM') "
						 */
						+ " AND " + statusID + " order by subcoDemandId desc";
			} else if (userBean != null && userBean.getUserWorkflowRole().trim().equals("GPRM")) {
				//statusID = " tl.demandStatus in ('102','117','120') ";
				statusID = " tl.demandStatus in ('102','117') ";
				sql = "select tl from TransactionLog tl where tl.gPracticeID in "
						+ "(select um.gPracticeID from User um where um.userID= :userID and um.userWorkflowRole='GPRM') "
						+ " AND " + statusID + " order by subcoDemandId desc";
			} else if (userBean != null && userBean.getUserWorkflowRole().trim().equals("OH")) {
				sql = "select tl from TransactionLog tl  where tl.demandStatus in ('103')";

			}
			if (sql != null && !sql.isEmpty()) {
				Query query = session.createQuery(sql);
				if (userBean != null && !userBean.getUserWorkflowRole().trim().equals("OH"))
					query.setParameter("userID", userBean.getUserID());
				query.setCacheable(true);
				query.setCacheRegion("getTransactionLogs");
				listOfTransactionLog = query.list();
			}

		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {

			if (session != null && session.isOpen())
				session.close();
		}
		return listOfTransactionLog;
	}

	/**
	 * This method receives gets initial filter data on basis of logged in user.
	 * 
	 * @param userBean
	 * 
	 * @return List<Country>
	 */
	public List<Country> getFiltersInitialData(UserBean userBean) {
		List<Country> country = null;
		Session session = sessionFactory.openSession();
		try {
			Query queryUsers = session.createQuery("select um from User um where um.userID = :userID");
			queryUsers.setParameter("userID", userBean.getUserID());
			List<User> users = queryUsers.list();

			Query query = session.createQuery("select cm from Country cm "
					+ "where  cm.gbuID in (select um.gbuId from User um " + "where um.userWorkflowRole=:userRole)");
			query.setParameter("userRole", users.get(0).getUserWorkflowRole());
			query.setCacheable(true);
			query.setCacheRegion("getFiltersInitialData");
			country = query.list();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return country;
	}

	/**
	 * This method received transaction log table data on selection of filters.
	 * 
	 * @param selectedGbuValue
	 * @param selectedCountryValue
	 * @param selectedGlobalPracticeValue
	 * @param selectedAgreementTypeValue
	 * @param selectedGCMValue
	 * @param selectedAssisgmentTypeValue
	 * @param selectedProjectMarginValue
	 * @param selectedAverageCostValue
	 * @param userRole
	 * @return List<TransactionLog>
	 */
	public List<TransactionLog> getTransactionLogsForFilters(String selectedGbuValue, String selectedCountryValue,
			String selectedGlobalPracticeValue, String selectedAgreementTypeValue, String selectedGCMValue,
			String selectedStatus, String selectedProjectMarginValue, String selectedAverageCostValue, String userRole,
			UserBean userBean) {

		String stringMargin = "";
		if (selectedProjectMarginValue != null && !selectedProjectMarginValue.isEmpty()) {
			if (selectedProjectMarginValue.contains("%")) {
				selectedProjectMarginValue = selectedProjectMarginValue.split("%")[0];
			}

			if (selectedProjectMarginValue.contains("<")) {
				selectedProjectMarginValue = selectedProjectMarginValue.replace("<", "").trim();
				Double margin = Double.parseDouble(selectedProjectMarginValue);
				selectedProjectMarginValue = margin.toString();
				stringMargin = "tl.margin < :margin";
			} else if (selectedProjectMarginValue.contains(">")) {
				selectedProjectMarginValue = selectedProjectMarginValue.replace(">", "").trim();
				Double margin = Double.parseDouble(selectedProjectMarginValue);
				selectedProjectMarginValue = margin.toString();
				stringMargin = "tl.margin > :margin";
			} else if (selectedProjectMarginValue.contains("-")) {
				StringTokenizer stringTokenizer = new StringTokenizer(selectedProjectMarginValue, "-");
				String first_value = stringTokenizer.nextToken();
				String second_value = stringTokenizer.nextToken();
				Double margin = Double.parseDouble(first_value);
				first_value = margin.toString();
				Double marginT = Double.parseDouble(second_value);
				second_value = marginT.toString();
				stringMargin = "tl.margin between " + first_value + " and " + second_value;
			}
		}

		String stringAvgCost = "";
		if (selectedAverageCostValue != null && !selectedAverageCostValue.isEmpty()) {
			if (selectedAverageCostValue.contains("<")) {
				selectedAverageCostValue = selectedAverageCostValue.replace("<", "").trim();
				Double avg = Double.parseDouble(selectedAverageCostValue);
				selectedAverageCostValue = avg.toString();
				stringAvgCost = "tl.avgDailyCost <= :avgDailyCost";
			} else if (selectedAverageCostValue.contains(">")) {
				selectedAverageCostValue = selectedAverageCostValue.replace(">", "").trim();
				Double avg = Double.parseDouble(selectedAverageCostValue);
				selectedAverageCostValue = avg.toString();
				stringAvgCost = "tl.avgDailyCost > :avgDailyCost";
			} else if (selectedAverageCostValue.contains("-")) {
				StringTokenizer stringTokenizer = new StringTokenizer(selectedAverageCostValue, "-");
				String first_value = stringTokenizer.nextToken();
				String second_value = stringTokenizer.nextToken();
				Double avg = Double.parseDouble(first_value);
				first_value = avg.toString();
				Double avgT = Double.parseDouble(second_value);
				second_value = avgT.toString();
				stringAvgCost = "tl.margin between " + first_value + " and " + second_value;
			}
		}

		List<TransactionLog> listOfTransactionLog = null;
		Session session = sessionFactory.openSession();
		Query query = null;
		Boolean flag = false;
		Boolean gbuFlag = false, countryFlag = false, gPracticeFlag = false, agmtTypeFlag = false, gcmFlag = false;

		try {
			StringBuffer dynamicQuery = new StringBuffer();

			// dynamicQuery.append("select tl from TransactionLog tl ");
			String sql = "";
			if (userBean != null && userBean.getUserWorkflowRole().trim().equals("PM")) {

				sql = "select tl from TransactionLog tl where tl.gBUID in "
						+ "(select um.gbuId from User um where um.userID= :userID and um.userWorkflowRole='PM') "
						+ "AND tl.countryID in (select um.countryID from User um where um.userID= :userID "
						+ "and um.userWorkflowRole='PM') "
						+ "and tl.gPracticeID in (select um.gPracticeID from User um where um.userID= :userID "
						+ "and um.userWorkflowRole='PM') ";

				// + " order by subcoDemandId desc";
				flag = true;
			} else if (userBean != null && userBean.getUserWorkflowRole().trim().equals("PRM")) {
				sql = "select tl from TransactionLog tl where tl.gBUID in "
						+ "(select um.gbuId from User um where um.userID= :userID and um.userWorkflowRole='PRM') ";
				/*
				 * +
				 * "AND tl.gPracticeID in (select um.gPracticeID from User um where um.userID= :userID "
				 * + "and um.userWorkflowRole='PRM') ";
				 */
				flag = true;
			} else if (userBean != null && userBean.getUserWorkflowRole().trim().equals("GPRM")) {
				sql = "select tl from TransactionLog tl where tl.gPracticeID in "
						+ "(select um.gPracticeID from User um where um.userID= :userID and um.userWorkflowRole='GPRM') ";
				flag = true;
			} else if (userBean != null && userBean.getUserWorkflowRole().trim().equals("OH")) {
				sql = "select tl from TransactionLog tl ";
				flag = false;

			}
			dynamicQuery.append(sql);
			if ((selectedGbuValue.equals("")) && (selectedCountryValue.equals(""))
					&& (selectedGlobalPracticeValue.equals("")) && (selectedAgreementTypeValue.equals(""))
					&& (selectedGCMValue.equals("")) && (selectedStatus.equals(""))
					&& (selectedProjectMarginValue.equals("")) && (selectedAverageCostValue.equals(""))) {
				// when everything ALL

			} else {

				// dynamicQuery.append(" where ");
				if (userBean != null && userBean.getUserWorkflowRole().trim().equals("OH")) {

					if (selectedGbuValue != null && !selectedGbuValue.isEmpty() && !selectedGbuValue.equals("null")) {
						if (flag) {
							dynamicQuery.append(" and ");
						} else {
							dynamicQuery.append(" where ");
						}
						String gbu = " tl.gBUID in (select c.gbuID from Country c where c.gbuName = :gbuname) ";
						dynamicQuery.append(gbu);
						flag = true;
						gbuFlag = true;
					}
					if (selectedCountryValue != null && !selectedCountryValue.isEmpty()
							&& !selectedCountryValue.equals("null")) {
						if (flag) {
							dynamicQuery.append(" and ");
						} else {
							dynamicQuery.append(" where ");
						}
						String country = " tl.countryID in (select c.countryID from Country c "
								+ "where c.countryName=:countryName) ";
						dynamicQuery.append(country);
						flag = true;
						countryFlag = true;
					}
					if (selectedGlobalPracticeValue != null && !selectedGlobalPracticeValue.isEmpty()
							&& !selectedGlobalPracticeValue.equals("null")) {
						if (flag) {
							dynamicQuery.append(" and ");
						} else {
							dynamicQuery.append(" where ");
						}
						String globalPractice = " tl.gPracticeID in (select c.gPracticeID from CountryPractise c "
								+ "where c.gPracticeName = :gPracticeName) ";
						dynamicQuery.append(globalPractice);
						flag = true;
						gPracticeFlag = true;
					}
				}
				if (userBean != null && userBean.getUserWorkflowRole().trim().equals("GPRM")) {
					if (selectedGbuValue != null && !selectedGbuValue.isEmpty() && !selectedGbuValue.equals("null")) {
						if (flag) {
							dynamicQuery.append(" and ");
						} else {
							dynamicQuery.append(" where ");
						}
						String gbu = " tl.gBUID in (select c.gbuID from Country c where c.gbuName = :gbuname) ";
						dynamicQuery.append(gbu);
						flag = true;
						gbuFlag = true;
					}
					if (selectedCountryValue != null && !selectedCountryValue.isEmpty()
							&& !selectedCountryValue.equals("null")) {
						if (flag) {
							dynamicQuery.append(" and ");
						} else {
							dynamicQuery.append(" where ");
						}
						String country = " tl.countryID in (select c.countryID from Country c "
								+ "where c.countryName=:countryName) ";
						// System.out.println("country = "+country);
						dynamicQuery.append(country);
						flag = true;
						countryFlag = true;
					}

					/*
					 * if(selectedGlobalPracticeValue != null &&
					 * !selectedGlobalPracticeValue.isEmpty() &&
					 * !selectedGlobalPracticeValue.equals("null") ) { if(flag)
					 * { dynamicQuery.append(" and "); }else {
					 * dynamicQuery.append(" where "); } String globalPractice =
					 * " tl.gPracticeID in (select c.gPracticeID from CountryPractise c "
					 * + "where c.gPracticeName = :gPracticeName) ";
					 * dynamicQuery.append(globalPractice); flag = true;
					 * gPracticeFlag = true; }
					 */
				}
				if (userBean != null && userBean.getUserWorkflowRole().trim().equals("PRM")) {

					if (selectedCountryValue != null && !selectedCountryValue.isEmpty()
							&& !selectedCountryValue.equals("null")) {
						if (flag) {
							dynamicQuery.append(" and ");
						} else {
							dynamicQuery.append(" where ");
						}
						String country = " tl.countryID in (select c.countryID from Country c "
								+ "where c.countryName=:countryName) ";
						// System.out.println("country = "+country);
						dynamicQuery.append(country);
						flag = true;
						countryFlag = true;
					}
					if (selectedGlobalPracticeValue != null && !selectedGlobalPracticeValue.isEmpty()
							&& !selectedGlobalPracticeValue.equals("null")) {
						if (flag) {
							dynamicQuery.append(" and ");
						} else {
							dynamicQuery.append(" where ");
						}
						String globalPractice = " tl.gPracticeID in (select c.gPracticeID from CountryPractise c "
								+ "where c.gPracticeName = :gPracticeName) ";
						dynamicQuery.append(globalPractice);
						flag = true;
						gPracticeFlag = true;
					}
				}
				if (selectedAgreementTypeValue != null && !selectedAgreementTypeValue.isEmpty()
						&& !selectedAgreementTypeValue.equals("null")) {
					if (flag) {
						dynamicQuery.append(" and ");
					} else {
						dynamicQuery.append(" where ");
					}
					String agmtType = " tl.subCoAgreement=:subCoAgreement ";
					dynamicQuery.append(agmtType);
					flag = true;
					agmtTypeFlag = true;
				}
				if (selectedGCMValue != null && !selectedGCMValue.isEmpty() && !selectedGCMValue.equals("null")) {
					if (flag) {
						dynamicQuery.append(" and ");
					} else {
						dynamicQuery.append(" where ");
					}
					String gcmValue = " tl.gcm=:gcm ";

					dynamicQuery.append(gcmValue);
					flag = true;
					gcmFlag = true;
				}
				if (selectedStatus != null) {
					String status = "";
					userRole = userRole.trim();
					if (!selectedStatus.isEmpty()) {
						if (flag) {
							dynamicQuery.append(" and ");

						} else {
							dynamicQuery.append(" where ");
						}

						if (selectedStatus.contains("New")) {
							if (userRole.equals("PM")) {
								status = " tl.demandStatus in ('101') ";
							} else if (userRole.equals("PRM")) {
								status = " tl.demandStatus in ('101') ";
							} else if (userRole.equals("GPRM")) {
								status = "tl.demandStatus in ('101')";
							} else if (userRole.equals("OH")) {
								status = "tl.demandStatus in ('101')";
							}

						} else if (selectedStatus.contains("Approved")) {
							if (userRole.equals("PM")) {
								status = " tl.demandStatus in ('102','103','104') ";
							} else if (userRole.equals("PRM")) {
								status = " tl.demandStatus in ('102','103','104') ";
							} else if (userRole.equals("GPRM")) {
								status = "tl.demandStatus in ('103')";
							} else if (userRole.equals("OH")) {
								status = "tl.demandStatus in ('102','103','104')";
							}

						} else if (selectedStatus.contains("Rejected")) {
							if (userRole.equals("PM")) {
								status = " tl.demandStatus in ('105','106','107') ";
							} else if (userRole.equals("PRM")) {
								status = " tl.demandStatus in ('105','106','107') ";
							} else if (userRole.equals("GPRM")) {
								status = "tl.demandStatus in ('106')";
							} else if (userRole.equals("OH")) {
								status = "tl.demandStatus in ('105','106','107')";
							}

						} else if (selectedStatus.contains("Extension")) {
							if (userRole.equals("PM")) {
								status = " tl.demandStatus in ('108','111','113','112','114','118') ";
							} else if (userRole.equals("PRM")) {
								status = " tl.demandStatus in ('108','109','110','111','112','113','114','118','119','120') ";
							} else if (userRole.equals("GPRM")) {
								status = "tl.demandStatus in ('109','110','113','120')";
							} else if (userRole.equals("OH")) {
								status = "tl.demandStatus in ('108','109','110','111','112','113','114','118','119','120')";
							}
						} else if (selectedStatus.contains("On Hold")) {
							if (userRole.equals("PM")) {
								status = " tl.demandStatus in ('115') ";
							} else if (userRole.equals("PRM")) {
								status = " tl.demandStatus in ('115','116','117') ";
							} else if (userRole.equals("GPRM")) {
								status = "tl.demandStatus in ('117')";
							} else if (userRole.equals("OH")) {
								//status = "tl.demandStatus in ('108','109','110','111','112','113','114')";
								status = "tl.demandStatus in ('115','116','117')";
							}
						}

					} else if (selectedStatus.isEmpty()) {
						if (flag) {
							dynamicQuery.append(" and ");

						} else {
							dynamicQuery.append(" where ");
						}
						if (userRole.equals("PM")) {
							//status = " tl.demandStatus in ('101','111','104') ";
							status = " tl.demandStatus in ('101','115','104') "; 
						} else if (userRole.equals("PRM")) {
							status = " tl.demandStatus in ('101') ";
						} else if (userRole.equals("GPRM")) {
							status = "tl.demandStatus in ('102')";
						} else if (userRole.equals("OH")) {
							status = "tl.demandStatus in ('103')";
						}
					}
					dynamicQuery.append(status);
					flag = true;
				}
				if (selectedProjectMarginValue != null && !selectedProjectMarginValue.isEmpty()
						&& !selectedProjectMarginValue.equals("null")) {
					if (flag) {
						dynamicQuery.append(" and ");
					} else {
						dynamicQuery.append(" where ");
					}
					dynamicQuery.append(stringMargin);
					flag = true;
				}
				if (selectedAverageCostValue != null && !selectedAverageCostValue.isEmpty()
						&& !selectedAverageCostValue.equals("null")) {
					if (flag) {
						dynamicQuery.append(" and ");
					} else {
						dynamicQuery.append(" where ");
					}
					dynamicQuery.append(stringAvgCost);
					flag = true;
				}
			}
			if (dynamicQuery != null && !dynamicQuery.equals("")) {

				dynamicQuery.append(" order by subcoDemandId desc");
				log.debug("dynamicQuery :" + dynamicQuery);
				query = session.createQuery(dynamicQuery.toString());

				if (gbuFlag)
					query.setParameter("gbuname", selectedGbuValue);
				if (countryFlag)
					query.setParameter("countryName", selectedCountryValue);
				if (agmtTypeFlag)
					query.setParameter("subCoAgreement", selectedAgreementTypeValue);
				if (gcmFlag)
					query.setParameter("gcm", selectedGCMValue);
				if (gPracticeFlag)
					query.setParameter("gPracticeName", selectedGlobalPracticeValue);

				if (stringMargin != null && !stringMargin.isEmpty()) {
					if (!stringMargin.contains("between"))
						query.setParameter("margin", selectedProjectMarginValue);
				}
				if (stringAvgCost != null && !stringAvgCost.isEmpty()) {
					if (!stringAvgCost.contains("between"))
						query.setParameter("avgDailyCost", selectedAverageCostValue);
				}
				if (userBean != null && !userBean.getUserWorkflowRole().trim().equals("OH")) {
					query.setParameter("userID", userBean.getUserID());
				}
			}

			if (query != null) {
				log.info("Query: " + query.toString());
				query.setCacheable(true);
				query.setCacheRegion("getTransactionLogsForFilters");
				listOfTransactionLog = query.list();
				if (listOfTransactionLog.isEmpty()) {
					listOfTransactionLog = new ArrayList<TransactionLog>();
				}
			} else {
				listOfTransactionLog = new ArrayList<TransactionLog>();
			}

		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return listOfTransactionLog;
	}

	/**
	 * This method gets gbu and country details from database.
	 * 
	 * @param Gbuid
	 * @param countryId
	 * @return List<Country>
	 */
	@SuppressWarnings("unchecked")
	public List<Country> getGBUIDAndCountryName(String Gbuid, String countryId) {
		List<Country> listOfCountryForGBU = null;
		Session session = sessionFactory.openSession();
		try {

			Query query = session.createQuery("select cm from Country cm, TransactionLog tl "
					+ "where tl.gBUID = cm.gbuID  AND tl.countryID = cm.countryID AND tl.gBUID=:gBUID "
					+ "and tl.countryID=:countryID");
			query.setParameter("gBUID", Gbuid);
			query.setParameter("countryID", countryId);
			//query.setCacheable(true);
			//query.setCacheRegion("getGBUIDAndCountryName");
			listOfCountryForGBU = query.list();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}

		return listOfCountryForGBU;
	}

	// duplicate method, but diff query
	/**
	 * This method gets manager details on selected SubcoDas ID
	 * 
	 * @param subconDasID
	 * 
	 * @return List<Manager>
	 */
	@SuppressWarnings("unchecked")
	public List<Manager> getManagerDetails(int subconDasID) {
		List<Manager> listOfManagerDetails = null;
		Session session = sessionFactory.openSession();
		try {

			Query query = session.createQuery("select mm from Manager mm, TransactionLog tl "
					+ "where tl.subcoDemandId = mm.subcoDemandID AND  tl.subcoDemandId=:subcoDemandID");
			query.setParameter("subcoDemandID", subconDasID);
			query.setCacheable(true);
			query.setCacheRegion("getManagerDetails");
			listOfManagerDetails = query.list();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}

		return listOfManagerDetails;
	}

	/**
	 * This method country practice details from database on gpractice id.
	 * 
	 * @param gPracticeID
	 * @return List<CountryPractise>
	 */
	@SuppressWarnings("unchecked")
	public List<CountryPractise> getCountryPractiseDetails(String gPracticeID, String subPracticeID, String countryID) {
		List<CountryPractise> listOfCountryPractsieDetails = null;
		Session session = sessionFactory.openSession();
		try {

			Query query = session.createQuery("select cp from CountryPractise cp "
					+ "where cp.gPracticeID=:gPracticeID and cp.subPracticeID=:subPracticeID and cp.countryID=:countryID");
			query.setParameter("gPracticeID", gPracticeID);
			query.setParameter("subPracticeID", subPracticeID);
			query.setParameter("countryID", countryID);
			query.setCacheable(true);
			query.setCacheRegion("getCountryPractiseDetails");
			listOfCountryPractsieDetails = query.list();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return listOfCountryPractsieDetails;
	}

	// ********variable name not correct chnages in controller too
	/**
	 * This method finds all approval data from Transaction log table.
	 * 
	 * @param listsubCoid1
	 * @return
	 * @return List<Object[]>
	 */
	public List<Object[]> findAll_ApprovalData(List<Integer> listsubCoid1) {
		Session session = sessionFactory.openSession();
		List<Object[]> result = null;
		try {

			Query query = session.createQuery("select distinct trsc.subcoDemandId , trsc.gBUID ,  coun.countryName , "
					+ "cp.gPracticeName, trsc.gcm , trsc.margin , trsc.avgDailyCost,trsc.subcoEDateWithAtos, "
					+ "trsc.subcoExtnRemark,trsc.subcoExtnEdate,trsc.subCoDateWithAtos from TransactionLog trsc , "
					+ "Country coun , CountryPractise cp WHERE trsc.gBUID = coun.gbuID "
					+ "AND trsc.countryID =coun.countryID AND coun.countryID =cp.countryID "
					+ "AND trsc.gPracticeID=cp.gPracticeID AND "
					+ "trsc.subPracticeID=cp.subPracticeID  AND trsc.subcoDemandId in  (:listsubCoid1)");
			query.setParameterList("listsubCoid1", listsubCoid1);
			query.setCacheable(true);
			query.setCacheRegion("findAll_ApprovalData");
			result = query.list();
			log.info("Query result Size = " + result.size());
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return result;
	}

	/**
	 * This method gets Latest approval data from database.
	 * 
	 * @param subcoId
	 * @return List<Object[]>
	 */

	public List<SubcoApprovalTransLog> getLastestApprovalData(List<Integer> subcoId) {
		Session session = sessionFactory.openSession();
		List<SubcoApprovalTransLog> result2 = null;
		try {
			Query query2 = session.createQuery(" select apprtra from  SubcoApprovalTransLog apprtra "
					+ "	WHERE apprtra.subcoDemandId in (:subcoId) order by apprtra.subcoDemandId desc  ");
			query2.setParameterList("subcoId", subcoId);
			query2.setCacheable(true);
			query2.setCacheRegion("getLastestApprovalData");
			result2 = query2.list();
			log.info("Latest Approval result = " + result2.size());
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return result2;
	}

	/**
	 * This method gets history data from database.
	 * 
	 * @param subcoId
	 * @return List<Object[]>
	 */
	public List<Object[]> getHistoryData(List<Integer> subcoId) {
		Session session = sessionFactory.openSession();
		List<Object[]> result = null;
		List<Object[]> result2 = null;
		try {

			Query query = session.createQuery("select shistory.subcoDemandId , trsc.subcoFName, coun.countryName , "
					+ "cp.gPracticeName, " + "shistory.approval, shistory.apprejdate, trsc.gcm, trsc.margin , "
					+ "trsc.avgDailyCost,shistory.apprejremark , shistory.apprrejdasid, "
					+ "shistory.apprejrole,shistory.apprrejFname,shistory.apprejLname from TransactionLog trsc ,SubcoApprovalTransLog apprtra,  "
					+ "Country coun , CountryPractise cp,SubconHistory shistory "
					+ "WHERE trsc.gBUID = coun.gbuID AND trsc.countryID=coun.countryID "
					+ "AND trsc.gPracticeID=cp.gPracticeID AND trsc.subPracticeID=cp.subPracticeID "
					+ "AND trsc.subcoDemandId = apprtra.subcoDemandId AND "
					+ "trsc.subcoDemandId = shistory.subcoDemandId AND "
					+ "coun.countryID =cp.countryID AND shistory.subcoDemandId in (:subcoId) "
					+ "order by shistory.apprejdate desc ");

			query.setParameterList("subcoId", subcoId);

			Query query2 = session.createQuery(" select apprtra.subcoDemandId , "
					+ "trsc.subcoFName, coun.countryName , cp.gPracticeName, apprtra.Approval, "
					+ "apprtra.ApprRej_date, trsc.gcm, trsc.margin , trsc.avgDailyCost, "
					+ "apprtra.ApprRej_Remark , apprtra.ApprRej_DASID, apprtra.ApprRej_Role, "
					+ "apprtra.ApprRej_FName, apprtra.ApprRej_LName "
					+ "from TransactionLog trsc , SubcoApprovalTransLog apprtra, Country coun , "
					+ " CountryPractise cp " + "	WHERE trsc.gBUID = coun.gbuID "
					+ "AND trsc.countryID=coun.countryID " + " AND trsc.gPracticeID=cp.gPracticeID "
					+ " AND trsc.subPracticeID=cp.subPracticeID " + " AND trsc.subcoDemandId = apprtra.subcoDemandId "
					+ "AND coun.countryID =cp.countryID "
					+ " AND apprtra.subcoDemandId in (:subcoId) order by apprtra.ApprRej_date desc  ");
			query2.setParameterList("subcoId", subcoId);
			query.setCacheable(true);
			query.setCacheRegion("getHistoryData");
			result = query.list();
			query2.setCacheable(true);
			query2.setCacheRegion("getHistoryData1");
			result2 = query2.list();
			result2.addAll(result);
			// result = query.list();
			log.info("history result = " + result.size());
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return result2;
	}

	/**
	 * This method saves approval or rejection data into database.
	 * 
	 * @param subconApprovaltranlog
	 * @param demandStatusId
	 * @param userId
	 * @return void
	 */
	public void saveApprovalOrRejection(SubcoApprovalTransLog subconApprovaltranlog, String demandStatusId,
			String userId) {
		Session session = sessionFactory.openSession();
		try {

			Transaction trx = session.getTransaction();
			trx.begin();

			TransactionLog log = (TransactionLog) session.get(TransactionLog.class,
					subconApprovaltranlog.getSubcoDemandId());
			log.setSubcoExtnRemark(subconApprovaltranlog.getApprRej_Remark());
			log.setDemandStatus(demandStatusId);
			log.setUpdatedBy(userId);
			log.setUpdatedDate(new Date());

			session.saveOrUpdate(subconApprovaltranlog);
			trx.commit();

		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
	}

	/**
	 * This method saves approval/rejection details in subconTransaction log
	 * table.
	 * 
	 * @param subconApprovaltranlog
	 * @return void
	 */
	public boolean saveApprovalTransLog(SubcoApprovalTransLog subconApprovaltranlog) {
		Session session = sessionFactory.openSession();
		Transaction trx = session.getTransaction();
		try {
			trx.begin();
			/*
			 * TransactionLog log =(TransactionLog)
			 * session.get(TransactionLog.class ,
			 * subconApprovaltranlog.getSubcoDemandId());
			 * log.setSubcoExtnRemark(subconApprovaltranlog.getApprRej_Remark())
			 * ;
			 */
			session.saveOrUpdate(subconApprovaltranlog);
			trx.commit();
		} catch (Exception e) {
			log.error(e.getMessage());
			trx.rollback();
			log.error("Insert failed in subconApprovaltranlog");
			return false;
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return true;
	}

	/**
	 * This method gets user data from userMaster table.
	 * 
	 * @param userID
	 * @return
	 * @return List<Object[]>
	 */
	public List<Object[]> getUsrData(String userID) {
		Session session = sessionFactory.openSession();
		List<Object[]> result = null;
		try {
			Query query = session.createQuery("select usr.userFName, usr.userLName, "
					+ "usr.userWorkflowRole, usr.updatedBy, usr.emailID from User usr where usr.userID in  (:userID)");

			query.setParameter("userID", userID);
			query.setCacheable(true);
			query.setCacheRegion("getUsrData");
			result = query.list();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return result;
	}

	/**
	 * This method updates extension details with role of logged in user and
	 * respective Subco demand Id into transaction log table o database.
	 * 
	 * @param subCoDasId
	 * @param date
	 * @param staretdate1
	 * @param role1
	 * @param diffInDaysinString
	 * @return void
	 */
	public void updateExtnDate(int subCoDasId, Date date, Date staretdate1, String role1, String diffInDaysinString) {
		log.info("updateExtnDate date = " + date);
		Session session = sessionFactory.openSession();
		try {
			TransactionLog log = (TransactionLog) session.get(TransactionLog.class, subCoDasId);
			Transaction trx = session.getTransaction();
			trx.begin();
			log.setSubcoExtnEdate(date);
			log.setSubcoExtnSdate(DateUtils.addDays(staretdate1, 1));
			log.setSubcoDuration(diffInDaysinString);
			log.setSubcoEDateWithAtos(date);
			trx.commit();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
	}

	/**
	 * This method updates extension details without role of logged in user and
	 * respective Subco demand Id into transaction log table o database.
	 * 
	 * @param subCoDasId
	 * @param date
	 * @param staretdate1
	 * @param diffInDaysinString
	 * @return void
	 */
	public void updateExtnDate(int subCoDasId, Date date, Date staretdate1, String diffInDaysinString) {
		log.info("updateExtnDate date without role = " + date);
		Session session = sessionFactory.openSession();
		try {
			TransactionLog log = (TransactionLog) session.get(TransactionLog.class, subCoDasId);
			Transaction trx = session.getTransaction();
			trx.begin();
			log.setSubcoExtnEdate(date);
			log.setSubcoExtnSdate(DateUtils.addDays(staretdate1, 1));
			log.setSubcoDuration(diffInDaysinString);
			// log.setSubcoEDateWithAtos(date);
			// log.setSubcoEDateWithAtos(date);
			// log.setSubcoExtnRemark(extnRemark);
			trx.commit();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
	}

	/**
	 * This method updates approval/rejection remarks of respective Subco demand
	 * Id into transaction log and subco Approval Transaction log tables o
	 * database.
	 * 
	 * @param subCoDasId
	 * @param appRemark
	 * @return void
	 */
	public void updateapprRemark(int subCoDasId, String appRemark) {
		Session session = sessionFactory.openSession();
		try {
			TransactionLog log = (TransactionLog) session.get(TransactionLog.class, String.valueOf(subCoDasId));
			SubcoApprovalTransLog log1 = (SubcoApprovalTransLog) session.get(SubcoApprovalTransLog.class,
					String.valueOf(subCoDasId));
			Transaction trx = session.getTransaction();
			trx.begin();
			log.setSubcoExtnRemark(appRemark);
			log1.setApprRej_Remark(appRemark);
			trx.commit();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
	}

	/**
	 * This method updates user act flag after successful login of user in
	 * database.
	 * 
	 * @param userBean
	 * @return void
	 */
	public void updateActFlag(User userBean) {
		Session session = sessionFactory.openSession();
		try {
			User log = (User) session.get(User.class, userBean.getUserID());
			Transaction trx = session.getTransaction();
			trx.begin();
			log.setUserActFlag("1");
			trx.commit();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}

	}

	/**
	 * This method updates user act flag after user's successful logout.
	 * 
	 * @param userID
	 * @return void
	 */
	public void updateActFlaginlogout(String userID) {
		Session session = sessionFactory.openSession();
		try {
			User log = (User) session.get(User.class, userID);
			Transaction trx = session.getTransaction();
			trx.begin();
			log.setUserActFlag("0");
			trx.commit();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
	}

	/**
	 * This method used to update extension remark into database.
	 * 
	 * @param subCoDasId
	 * @param appRemark
	 * @param demandStatusId
	 * @param userId
	 * @param subconApprovaltranlog
	 * @return void
	 */
	public void updateextnRemark(int subCoDasId, String appRemark, String demandStatusId, String userId,
			SubcoApprovalTransLog subconApprovaltranlog) {
		Session session = sessionFactory.openSession();
		try {
			TransactionLog log = (TransactionLog) session.get(TransactionLog.class, subCoDasId);
			SubcoApprovalTransLog log1 = (SubcoApprovalTransLog) session.get(SubcoApprovalTransLog.class, subCoDasId);
			Transaction trx = session.getTransaction();
			trx.begin();
			log.setSubcoExtnRemark(appRemark);
			log.setDemandStatus(demandStatusId);
			log.setUpdatedBy(userId);
			log.setUpdatedDate(new Date());
			log1.setApprRej_Remark(appRemark);
			log1.setUpdatedBy(userId);
			log1.setUpdatedDate(new Date());
			log1.setApprRej_date(new Date());
			log1.setApprRej_DASID(subconApprovaltranlog.getApprRej_DASID());
			log1.setApprRej_FName(subconApprovaltranlog.getApprRej_FName());
			log1.setApprRej_LName(subconApprovaltranlog.getApprRej_LName());
			log1.setApprRej_Role(subconApprovaltranlog.getApprRej_Role());
			log1.setApproval(subconApprovaltranlog.getApproval());

			// log1.setApprRej_Remark(appRemark);
			// session.saveOrUpdate(log);
			// session.saveOrUpdate(log1);
			trx.commit();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
	}

	/**
	 * This method used to update extension remark in Transaction log table.
	 * 
	 * @param damandStatusId
	 * @param appRemark
	 * @return void
	 */
	public void updateextnRemark(int damandStatusId, String appRemark) {
		Session session = sessionFactory.openSession();
		try {
			TransactionLog log = (TransactionLog) session.get(TransactionLog.class, damandStatusId);
			Transaction trx = session.getTransaction();
			trx.begin();
			log.setSubcoExtnRemark(appRemark);
			trx.commit();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
	}

	/**
	 * This method gets user role according to userID from database.
	 * 
	 * @param userID
	 * @return List<Object[]>
	 */
	public List<Object[]> getUsrRole(String userID) {
		Session session = sessionFactory.openSession();
		List<Object[]> result = null;
		try {
			Query query = session
					.createQuery("select usr.userWorkflowRole from User usr " + "where usr.userID = :userID");
			query.setParameter("userID", userID);
			query.setCacheable(true);
			query.setCacheRegion("getUsrRole");
			result = query.list();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return result;
	}

	/**
	 * This method userRole from PErsonalise table.
	 * 
	 * @param role1
	 * @return List<Personalise>
	 */
	public List<Personalise> getUserRole1(String role1) {
		Session session = sessionFactory.openSession();
		List<Personalise> result = null;
		try {
			Query query = session.createQuery("from Personalise where userRole = :userWorkflowRole");
			query.setParameter("userWorkflowRole", role1);
			query.setCacheable(true);
			query.setCacheRegion("getUserRole1");
			result = query.list();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return result;
	}

	/**
	 * This method gets user details
	 * 
	 * @param gbuName
	 * @param countryName
	 * @return List<Object[]>
	 */
	public List<Object[]> getGbuUsrData(String gbuName, String countryName) {
		Session session = sessionFactory.openSession();
		List<Object[]> result = null;
		try {
			countryName = countryName.replace("string:", "");
			Query query = session.createQuery("select usr.gbuID, usr.countryID from "
					+ "Country usr where usr.gbuName = :gbuName and usr.countryName = :countryName");
			query.setParameter("gbuName", gbuName);
			query.setParameter("countryName", countryName);
			query.setCacheable(true);
			query.setCacheRegion("getGbuUsrData");
			result = query.list();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return result;
	}

	/**
	 * This method
	 * 
	 * @param countryID
	 * @return List<Object[]>
	 */
	public List<Object[]> getCountryUsrData(String countryID) {
		Session session = sessionFactory.openSession();
		List<Object[]> result = null;
		try {
			Query query = session.createQuery(
					"select usr.gPracticeID from CountryPractise usr " + "where usr.countryID = :countryID");
			query.setParameter("countryID", countryID);
			query.setCacheable(true);
			query.setCacheRegion("getCountryUsrData");
			result = query.list();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return result;
	}

	// this method not being called from controller
	public String getCountryUsrData1(String countryID) {
		Session session = null;
		List<Object[]> result = null;
		String user = null;
		try {
			session = sessionFactory.openSession();
			Query query = session.createQuery(
					"select usr.gPracticeID from CountryPractise usr " + "where usr.countryID = :countryID");
			query.setParameter("countryID", countryID);
			query.setCacheable(true);
			query.setCacheRegion("getCountryUsrData1");
			user = (String) query.uniqueResult();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return user;
	}

	/**
	 * This method saves user in database.
	 * 
	 * @param countryID
	 * @return List<Object[]>
	 */
	public void save(User user) {
		Session session = sessionFactory.openSession();
		try {
			Transaction trx = session.getTransaction();
			trx.begin();
			session.save(user);
			trx.commit();

		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
	}

	/**
	 * This method saves user in database.
	 * 
	 * @param countryID
	 * @return List<Object[]>
	 */
	public void edit(User user) {
		Session session = sessionFactory.openSession();
		try {
			Transaction trx = session.getTransaction();
			trx.begin();
			session.saveOrUpdate(user);
			trx.commit();

		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
	}

	/**
	 * This method gets user details
	 * 
	 * @param userId
	 * @return List<User>
	 */
	public List<User> getUserMasterDetails(String userId) {
		Session session = sessionFactory.openSession();
		List<User> result = null;
		try {
			/*
			 * Query query = session.createQuery(
			 * "from User where createdBy = :userId and  " +
			 * "NOT userID=:userId order by createdDate");
			 */
			Query query = session.createQuery("from User where createdBy = :userId order by createdDate");
			query.setParameter("userId", userId);
			query.setCacheable(true);
			query.setCacheRegion("getUserMasterDetails");
			result = query.list();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return result;
	}

	/**
	 * This method edits usermaster details in database.
	 * 
	 * @param id
	 * @return List<User>
	 */
	public List<User> getEditMasterDetails(String id, String role) {
		Session session = sessionFactory.openSession();
		List<User> result = null;
		try {
			Query query = session.createQuery("from User where userID = :id and userWorkflowRole=:role");
			query.setParameter("id", id);
			query.setParameter("role", role);
			query.setCacheable(true);
			query.setCacheRegion("getEditMasterDetails");
			result = query.list();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return result;
	}

	/**
	 * This method gets created by from user master
	 * 
	 * @param userID
	 * @return String
	 */
	public String getUsrCreatedBy(String userID) {
		Session session = sessionFactory.openSession();
		String user = null;
		try {
			Query query = session.createQuery("select usr.createdBy from User usr where usr.userID = :userID");
			query.setParameter("userID", userID);
			query.setCacheable(true);
			query.setCacheRegion("getUsrCreatedBy");
			user = (String) query.uniqueResult();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return user;

	}

	/**
	 * This method removes user from database;
	 * 
	 * @param id
	 * @return void
	 */
	public void getremoveMasterDetails(String id, String role) {
		Session session = sessionFactory.openSession();
		try {
			Transaction trx = session.getTransaction();
			trx = session.beginTransaction();
			// Query query = session.createQuery("delete from User usr where
			// usr.userID = :id");
			User product = new User();
			product.setUserID(id);
			product.setUserWorkflowRole(role);
			session.delete(product);
			trx.commit();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
	}

	/**
	 * This method saves personalize field in personalize table
	 * 
	 * @param personalise
	 * @throws Exception
	 * @return void
	 */
	@Override
	public void savePersonalise(Personalise personalise) throws Exception {
		Transaction transaction = null;
		Session session = sessionFactory.openSession();
		try {
			transaction = session.beginTransaction();
			session.saveOrUpdate(personalise);
			transaction.commit();
		} catch (Exception e) {
			if (transaction != null)
				transaction.rollback();
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
	}

	/**
	 * This method gets gbu details
	 * 
	 * @param gbuId
	 * @param countryId
	 * @return List<Country>
	 */
	public List<Country> getGbuDetails(String gbuId, String countryId) {
		Session session = sessionFactory.openSession();
		List<Country> result = null;
		try {
			Query query = session.createQuery("from Country where gbuID = :gbuId and countryID = :countryId");
			query.setParameter("gbuId", gbuId);
			query.setParameter("countryId", countryId);
			query.setCacheable(true);
			query.setCacheRegion("getGbuDetails");
			result = query.list();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return result;
	}

	/**
	 * This method gets country practice details
	 * 
	 * @param countryID
	 * @param gPracticeName
	 * @return List<Object[]>
	 */
	public List<Object[]> getCountryPraDetails(String countryID, String gPracticeName, String subPracticeName) {
		Session session = sessionFactory.openSession();
		List<Object[]> result = null;
		try {
			countryID = countryID.replace("string:", "");
			gPracticeName = gPracticeName.replace("string:", "");
			Query query = session.createQuery("select usr.gPracticeID, usr.subPracticeID from "
					+ "CountryPractise usr where usr.countryID = :countryID and usr.gPracticeName = :gPracticeName and usr.subPractiseName = :subPractiseName");
			query.setParameter("countryID", countryID);
			query.setParameter("gPracticeName", gPracticeName);
			query.setParameter("subPractiseName", subPracticeName);
			query.setCacheable(true);
			query.setCacheRegion("getCountryPraDetails");
			result = query.list();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return result;
	}

	/**
	 * This method get country practice details
	 * 
	 * @param countryID
	 * @param subPracticeID
	 * @param gPracticeID
	 * @return List<CountryPractise>
	 */
	public List<CountryPractise> getCountryPraDetailsEdit(String countryID, String subPracticeID, String gPracticeID) {
		Session session = sessionFactory.openSession();
		List<CountryPractise> result = null;
		try {
			countryID = countryID.replace("string:", "");
			subPracticeID = subPracticeID.replace("string:", "");
			gPracticeID = gPracticeID.replace("string:", "");

			Query query = session.createQuery("from CountryPractise where countryID = :countryID "
					+ "and subPracticeID = :subPracticeID and gPracticeID = :gPracticeID ");
			query.setParameter("countryID", countryID);
			query.setParameter("subPracticeID", subPracticeID);
			query.setParameter("gPracticeID", gPracticeID);
			query.setCacheable(true);
			query.setCacheRegion("getCountryPraDetailsEdit");
			result = query.list();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return result;
	}

	/**
	 * This method saves approval/rejection details in subconTransaction log
	 * table.
	 * 
	 * @param subconApprovaltranlog
	 * @return void
	 */
	public void saveApprovalOrRejection(SubcoApprovalTransLog subconApprovaltranlog) {
		Session session = sessionFactory.openSession();
		try {
			Transaction trx = session.getTransaction();
			trx.begin();
			TransactionLog log = (TransactionLog) session.get(TransactionLog.class,
					subconApprovaltranlog.getSubcoDemandId());
			log.setSubcoExtnRemark(subconApprovaltranlog.getApprRej_Remark());
			session.saveOrUpdate(subconApprovaltranlog);
			trx.commit();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
	}

	/**
	 * This method updates Approve/rejection remark.
	 * 
	 * @param subCoDasId
	 * @param appRemark
	 * @param damandStatusId
	 * @param userId
	 * @param subconApprovaltranlog
	 * @return void
	 */
	public void updateapprRemark(int subCoDasId, String appRemark, String damandStatusId, String userId,
			SubcoApprovalTransLog subconApprovaltranlog) {
		Session session = sessionFactory.openSession();
		try {
			TransactionLog log = (TransactionLog) session.get(TransactionLog.class, subCoDasId);
			SubcoApprovalTransLog log1 = (SubcoApprovalTransLog) session.get(SubcoApprovalTransLog.class, subCoDasId);
			Transaction trx = session.getTransaction();
			trx.begin();
			// log.setSubcoExtnEdate(date);
			log.setSubcoExtnRemark(appRemark);
			log1.setApprRej_Remark(appRemark);
			log.setDemandStatus(damandStatusId);
			log.setUpdatedBy(userId);
			log.setUpdatedDate(new Date());
			log1.setUpdatedBy(userId);
			log1.setUpdatedDate(new Date());
			log1.setApprRej_date(new Date());
			log1.setApprRej_DASID(subconApprovaltranlog.getApprRej_DASID());
			log1.setApprRej_FName(subconApprovaltranlog.getApprRej_FName());
			log1.setApprRej_LName(subconApprovaltranlog.getApprRej_LName());
			log1.setApprRej_Role(subconApprovaltranlog.getApprRej_Role());
			log1.setApproval(subconApprovaltranlog.getApproval());
			trx.commit();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
	}

	/**
	 * This method gets Ou name from database.
	 * 
	 * @param ouid
	 * @return String
	 */
	public String getOuName(String ouid) {
		Session session = sessionFactory.openSession();
		String ouName = null;
		try {
			Query query = session.createQuery("select ouId from OuMaster where ouName=:ouid");
			query.setParameter("ouid", ouid);
			query.setCacheable(true);
			query.setCacheRegion("getOuName");
			ouName = (String) query.uniqueResult();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return ouName;
	}

	/**
	 * This method checks user's existing role from user Master
	 * 
	 * @param userID
	 * @return boolean
	 */
	public boolean checkUserExistWthRole(UserBean userBean) {
		Session session = sessionFactory.openSession();
		List<User> listOfUser = null;
		try {
			Query query = session.createQuery("from User where userID=:userID and userWorkflowRole=:userWorkflowRole");
			query.setParameter("userID", userBean.getUserID());
			query.setParameter("userWorkflowRole", userBean.getUserWorkflowRole());
			query.setCacheable(true);
			query.setCacheRegion("checkUserExistWthRole");
			listOfUser = query.list();
			if (listOfUser.size() == 1) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return false;
	}

	/**
	 * This method gets country sub practice details
	 * 
	 * @param gPractiseName
	 * @return List<String>
	 */
	@SuppressWarnings("unchecked")
	public List<String> getCountrySubPractices(String gPractiseName) {
		Session session = sessionFactory.openSession();
		List<String> listOfCountryPractise = null;
		try {
			listOfCountryPractise = session
					.createQuery("select distinct(cp.subPractiseName) "
							+ "from CountryPractise cp where cp.gPracticeName=:gPractiseName")
					.setParameter("gPractiseName", gPractiseName).list();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return listOfCountryPractise;
	}

	/**
	 * This method gets ou name for selected ouId
	 * 
	 * @param ouId
	 * @return String
	 */
	public String getOuNameforID(String ouId) {
		Session session = sessionFactory.openSession();
		String ouName = null;
		try {
			Query query = session.createQuery("select ouName from OuMaster where ouId=:ouId");
			query.setParameter("ouId", ouId);
			query.setCacheable(true);
			query.setCacheRegion("getOuNameforID");
			ouName = (String) query.uniqueResult();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return ouName;
	}

	/**
	 * 
	 * @param userID
	 * @param role
	 * @return
	 */

	public String getUsrRoleforLogin(String userID, String role) {
		String roleforLogin = "";
		Session session = sessionFactory.openSession();
		try {
			Query query = session.createQuery(
					"select userWorkflowRole from User where userID=:userID " + "and userWorkflowRole = :role");
			query.setParameter("role", role);
			query.setParameter("userID", userID);
			query.setCacheable(true);
			query.setCacheRegion("getUsrRoleforLogin");
			roleforLogin = (String) query.uniqueResult();
			log.info("User result getUsrRoleforLogin  = " + roleforLogin);
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return roleforLogin;
	}

	/**
	 * 
	 * @param userID
	 * @return
	 */
	public String resetPswd(String userID) {
		log.info("resetPswd userID  = " + userID);
		Session session = sessionFactory.openSession();
		String usrPwd = "";
		try {
			Query query = session.createQuery("select userPwd from User where userID=:userID");
			query.setParameter("userID", userID);
			query.setCacheable(true);
			query.setCacheRegion("resetPswd");
			usrPwd = (String) query.uniqueResult();
			log.info("User result usrPwd  = " + usrPwd);
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return usrPwd;
	}

	/**
	 * 
	 * @param userBean
	 * @param inputnewPassword
	 */
	public void updatePswd(UserBean userBean, String inputnewPassword) {
		Session session = sessionFactory.openSession();
		try {
			User log = (User) session.get(User.class, userBean.getUserID());
			Transaction trx = session.getTransaction();
			trx.begin();
			log.setUserPwd(inputnewPassword);
			trx.commit();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
	}

	/**
	 * 
	 * @param userID
	 * @return
	 */

	public String getUserEmailId(String userID) {
		log.info("resetPswd userID  = " + userID);
		Session session = sessionFactory.openSession();
		String usrEmailID = "";
		try {
			Query query = session.createQuery("select emailID from User where userID=:userID");
			query.setParameter("userID", userID);
			query.setCacheable(true);
			query.setCacheRegion("getUserEmailId");
			usrEmailID = (String) query.uniqueResult();
			log.info("User result usrEmailID  = " + usrEmailID);
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return usrEmailID.trim();
	}

	/**
	 * 
	 * @param userID
	 * @param user
	 */
	public void updatePswdforforgot(String userID, User user) {
		Session session = sessionFactory.openSession();
		try {
			User log = (User) session.get(User.class, userID);
			Transaction trx = session.getTransaction();
			trx.begin();
			log.setUserPwd(user.getUserPwd());
			log.setUpdatedDate(new Date());
			log.setUpdatedBy("Auto");
			trx.commit();
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
	}

	/**
	 * 
	 * @param subcoDemandId
	 * @return
	 */
	public String getSubcoDemandID(int subcoDemandId) {

		log.info("getSubcoDemandID demandId  = " + subcoDemandId);
		Session session = sessionFactory.openSession();
		String subConDasId = "";
		try {
			session.beginTransaction();
			Query query = session.createQuery(
					"select subcoDemandId from SubcoApprovalTransLog " + "where subcoDemandId=:subcoDemandId");
			query.setParameter("subcoDemandId", subcoDemandId);
			query.setCacheable(true);
			query.setCacheRegion("getSubcoDemandID");
			subConDasId = (String) query.uniqueResult();
			log.info("User result subConDasId  = " + subConDasId);
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return subConDasId.trim();
	}

	/**
	 * 
	 * @param userBean
	 * @return
	 */
	public List<Object[]> getCountriesPractisePRM(UserBean userBean) {

		Session session = sessionFactory.openSession();
		List<Object[]> result = null;
		try {

			if (userBean != null && userBean.getUserWorkflowRole().trim().equals("PRM")) {
				// System.out.println("PRM");

				Query query = session.createQuery("select distinct cp.gPracticeName from CountryPractise cp "
						+ "where cp.countryID in (select distinct um.countryID from User um where um.userID= :userID)");

				query.setParameter("userID", userBean.getUserID());
				query.setCacheable(true);
				query.setCacheRegion("getCountriesPractisePRM");
				result = query.list();
			}

		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return result;

	}

	/**
	 * 
	 * @param gbuName
	 * @return
	 */
/*	--------*/
	public List<CountryPractise> getCountriesForGBUGPRM(String gbuName) {

		List<CountryPractise> listOfCountryPractise = null;
		Session session = sessionFactory.openSession();
		try {
			listOfCountryPractise = (List<CountryPractise>) session
					.createQuery("select distinct(countryName) " + " from Country where gbuName=:gbu")
					.setParameter("gbu", gbuName).list();
			
			
			
			/*Query query = session.createQuery("select distinct(cpm.gPracticeName) "
					+ "from CountryPractise cpm, Country cm where cm.countryID=cpm.countryID "
					+ "and cpm.countryID IN (select cc.countryID from Country cc " + "where cc.countryName=:country)")
					.setParameter("country", country);
			
			listOfCountryForGBU = query.list();*/
			
			
			
			
			
			

		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (session != null && session.isOpen())
				session.close();
		}
		return listOfCountryPractise;
	}
}
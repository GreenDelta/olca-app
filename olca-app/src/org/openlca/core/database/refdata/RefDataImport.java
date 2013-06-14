package org.openlca.core.database.refdata;

import java.sql.Connection;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

public class RefDataImport {

	private EntityManagerFactory emf;

	public RefDataImport(EntityManagerFactory emf) {
		this.emf = emf;
	}

	public void importMappings() {
		EntityManager em = emf.createEntityManager();
		try (Connection con = em.unwrap(Connection.class)) {
			MappingImport mappingImport = new MappingImport(con);
			mappingImport.run();
			em.getTransaction().commit();
		} catch (Exception e) {
			em.getTransaction().rollback();
		} finally {
			em.close();
		}
	}

}

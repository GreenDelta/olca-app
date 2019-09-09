package org.openlca.app.db;

/**
 * Interface to describe remote connections
 *
 */
public interface IRemoteDatabaseConfiguration extends IDatabaseConfiguration {

  String getHost();

  int getPort();

  String getUser();

  String getPassword();
}

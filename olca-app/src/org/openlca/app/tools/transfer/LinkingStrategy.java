package org.openlca.app.tools.transfer;

/// Strategy for finding matching providers in the target database
/// when a product system is transferred.
public enum LinkingStrategy {

    /// Match providers only by reference ID.
    /// Use this when provider identities are expected to be stable
    /// across source and target databases.
    BY_ID,

    /// Match providers primarily by process name and location.
    /// If candidates with the same reference ID exist in the target
    /// database, prefer them over other matches.
    BY_NAME

}

import React, { createContext, useContext, ReactNode } from 'react';

// Context for URL state management
interface URLStateContextType {
  // This context can be extended if needed for global URL state management
  // For now, it's mainly for consistency with the original NuqsAdapter
  // and to provide a place for future URL state utilities
}

const URLStateContext = createContext<URLStateContextType | null>(null);

// URLStateAdapter component that replaces NuqsAdapter
export function URLStateAdapter({ children }: { children: ReactNode }) {
  // The actual URL state management is handled by the hooks
  // This component mainly provides the context structure
  // and ensures the URL state system is properly initialized
  const contextValue: URLStateContextType = {};

  return (
    <URLStateContext.Provider value={contextValue}>
      {children}
    </URLStateContext.Provider>
  );
}

// Hook to use the URL state context (if needed in the future)
export function useURLStateContext() {
  const context = useContext(URLStateContext);
  if (!context) {
    throw new Error('useURLStateContext must be used within URLStateAdapter');
  }
  return context;
}
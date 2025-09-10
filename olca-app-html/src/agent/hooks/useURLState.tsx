// src/hooks/useURLState.ts
import { useState, useEffect, useCallback } from 'react';

// Types for URL state management
type URLStateOptions<T> = {
  defaultValue?: T;
  parse?: (value: string) => T;
  serialize?: (value: T) => string;
};

// Boolean parser similar to parseAsBoolean
export const parseAsBoolean = {
  withDefault: (defaultValue: boolean) => ({
    parse: (value: string) => value === 'true',
    serialize: (value: boolean) => value.toString(),
    defaultValue,
  }),
};

// Main URL state hook - optimized for static HTML rendering
export function useQueryState<T = string>(
  key: string,
  options?: URLStateOptions<T>
): [T | null, (value: T | null) => void] {
  const {
    defaultValue,
    parse = (value: string) => value as T,
    serialize = (value: T) => String(value),
  } = options || {};

  // Get initial value from URL - simplified for static HTML
  const getInitialValue = useCallback((): T | null => {
    const urlParams = new URLSearchParams(window.location.search);
    const value = urlParams.get(key);
    
    if (value === null) return defaultValue || null;
    
    try {
      return parse(value);
    } catch {
      return defaultValue || null;
    }
  }, [key, parse, defaultValue]);

  const [state, setState] = useState<T | null>(getInitialValue);

  // Update URL when state changes
  const updateState = useCallback((newValue: T | null) => {
    const url = new URL(window.location.href);
    
    if (newValue === null || newValue === undefined) {
      url.searchParams.delete(key);
    } else {
      try {
        const serializedValue = serialize(newValue);
        url.searchParams.set(key, serializedValue);
      } catch {
        console.warn(`Failed to serialize value for key "${key}":`, newValue);
        return;
      }
    }

    // Update URL without triggering page reload
    window.history.pushState({}, '', url.toString());
    setState(newValue);
  }, [key, serialize]);

  // Listen for browser back/forward navigation
  useEffect(() => {
    const handlePopState = () => {
      const newValue = getInitialValue();
      setState(newValue);
    };

    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, [getInitialValue]);

  return [state, updateState];
}

// Convenience hook for boolean values
export function useBooleanQueryState(
    key: string,
    defaultValue: boolean = false
  ): [boolean, (value: boolean) => void] {
    const [value, setValue] = useQueryState(key, {
      parse: (val) => val === 'true',
      serialize: (val) => val.toString(),
      defaultValue,
    });
  
    return [value || defaultValue, setValue];
  }
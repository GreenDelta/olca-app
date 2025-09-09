declare module '*.css' {
  const content: string;
  export default content;
}

declare module '*.scss' {
  const content: string;
  export default content;
}

declare module '*.sass' {
  const content: string;
  export default content;
}

declare module '*.less' {
  const content: string;
  export default content;
}

declare module '*.png' {
  const content: string;
  export default content;
}

declare module '*.jpg' {
  const content: string;
  export default content;
}

declare module '*.jpeg' {
  const content: string;
  export default content;
}

declare module '*.gif' {
  const content: string;
  export default content;
}

declare module '*.svg' {
  const content: string;
  export default content;
}

declare module '*.webp' {
  const content: string;
  export default content;
}

// Webpack HMR types
interface NodeModule {
  hot?: {
    accept(): void;
    accept(dependency: string, callback: () => void): void;
    accept(dependencies: string[], callback: () => void): void;
    decline(): void;
    decline(dependency: string): void;
    decline(dependencies: string[]): void;
    dispose(callback: (data: any) => void): void;
    addDisposeHandler(callback: (data: any) => void): void;
    removeDisposeHandler(callback: (data: any) => void): void;
    check(autoApply?: boolean): Promise<string[] | null>;
    apply(options?: { ignoreUnaccepted?: boolean }): Promise<string[] | null>;
    status(): string;
    addStatusHandler(callback: (status: string) => void): void;
    removeStatusHandler(callback: (status: string) => void): void;
  };
}

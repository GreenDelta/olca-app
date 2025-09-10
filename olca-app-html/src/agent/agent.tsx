// App.tsx - This replaces both layout.tsx and page.tsx
import React from "react";
import { createRoot } from "react-dom/client";
import { Thread } from "./components/thread";
import { StreamProvider } from "./providers/Stream";
import { ThreadProvider } from "./providers/Thread";
import { ArtifactProvider } from "./components/thread/artifact";
import { Toaster } from "./components/ui/sonner";
import { URLStateAdapter } from "./providers/URLStateAdapter";
import "../styles/global.css";

function App(): React.ReactElement {
  return (
    <URLStateAdapter>
      <React.Suspense fallback={<div>Loading...</div>}>
        <Toaster />
        <ThreadProvider>
          <StreamProvider>
            <ArtifactProvider>
              <Thread />
            </ArtifactProvider>
          </StreamProvider>
        </ThreadProvider>
      </React.Suspense>
    </URLStateAdapter>
  );
}

// Initialize the app
const initializeApp = () => {
  const container = document.getElementById('app');
  if (container) {
    const root = createRoot(container);
    root.render(<App />);
  }
};

// Global functions for Java integration
declare global {
  interface Window {
    setTheme: (isDark: boolean) => void;
  }
}

// Expose theme function for Java integration
// This allows the Java app to control light/dark mode and opens the door for HTML <-> Java communication
window.setTheme = (isDark: boolean) => {
  document.body.className = isDark ? 'dark' : 'light';
};

// Initialize when DOM is ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initializeApp);
} else {
  initializeApp();
}

// Hot Module Replacement (HMR) support
if (module.hot) {
  module.hot.accept();
}

export default App;

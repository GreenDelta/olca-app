"use client"

import { TypingIndicator } from "@/components/ui/typing-indicator"

/**
 * Standalone typing indicator example.
 * 
 * This example shows how to use the TypingIndicator component
 * as a standalone element in various contexts.
 */
export function StandaloneTypingIndicator() {
  const [isGenerating, setIsGenerating] = React.useState(false)

  React.useEffect(() => {
    // Simulate AI generation
    const interval = setInterval(() => {
      setIsGenerating(prev => !prev)
    }, 4000)

    return () => clearInterval(interval)
  }, [])

  return (
    <div className="w-full max-w-2xl mx-auto p-4">
      <h3 className="text-lg font-semibold mb-4">Standalone Typing Indicator</h3>
      <p className="text-sm text-muted-foreground mb-4">
        Typing indicator used in various standalone contexts.
      </p>
      
      <div className="space-y-6">
        <div>
          <h4 className="text-sm font-medium mb-2">AI Generation Status</h4>
          <div className="flex items-center gap-2 p-4 bg-muted rounded-lg">
            {isGenerating ? (
              <>
                <TypingIndicator size="sm" />
                <span className="text-sm text-muted-foreground">AI is generating response...</span>
              </>
            ) : (
              <span className="text-sm text-muted-foreground">Ready to assist</span>
            )}
          </div>
        </div>

        <div>
          <h4 className="text-sm font-medium mb-2">Loading State</h4>
          <div className="flex items-center justify-center gap-2 p-8 bg-muted rounded-lg">
            <TypingIndicator size="md" color="primary" />
            <span className="text-sm text-muted-foreground">Processing your request...</span>
          </div>
        </div>

        <div>
          <h4 className="text-sm font-medium mb-2">Real-time Collaboration</h4>
          <div className="space-y-2">
            <div className="flex items-center gap-2 p-3 bg-muted rounded-lg">
              <div className="h-2 w-2 bg-green-500 rounded-full"></div>
              <span className="text-sm">John is online</span>
            </div>
            <div className="flex items-center gap-2 p-3 bg-muted rounded-lg">
              <TypingIndicator size="sm" />
              <span className="text-sm">Sarah is typing...</span>
            </div>
            <div className="flex items-center gap-2 p-3 bg-muted rounded-lg">
              <div className="h-2 w-2 bg-yellow-500 rounded-full"></div>
              <span className="text-sm">Mike is away</span>
            </div>
          </div>
        </div>

        <div>
          <h4 className="text-sm font-medium mb-2">LCA Analysis Progress</h4>
          <div className="flex items-center gap-2 p-4 bg-muted rounded-lg">
            <TypingIndicator size="sm" color="primary" />
            <span className="text-sm text-muted-foreground">Analyzing environmental impact data...</span>
          </div>
        </div>
      </div>
    </div>
  )
}

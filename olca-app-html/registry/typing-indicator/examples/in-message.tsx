"use client"

import { TypingIndicator } from "@/components/ui/typing-indicator"

/**
 * TypingIndicator integrated in message example.
 * 
 * This example shows how to use the TypingIndicator component
 * within message components to show when someone is typing.
 */
export function InMessageTypingIndicator() {
  const [isTyping, setIsTyping] = React.useState(false)

  React.useEffect(() => {
    // Simulate typing state changes
    const interval = setInterval(() => {
      setIsTyping(prev => !prev)
    }, 3000)

    return () => clearInterval(interval)
  }, [])

  return (
    <div className="w-full max-w-2xl mx-auto p-4">
      <h3 className="text-lg font-semibold mb-4">Typing Indicator in Message</h3>
      <p className="text-sm text-muted-foreground mb-4">
        Typing indicator integrated within a message bubble. The typing state changes every 3 seconds.
      </p>
      
      <div className="space-y-4">
        <div className="flex items-start gap-2">
          <div className="bg-muted rounded-lg px-4 py-2">
            <p className="text-sm">Hello! How can I help you with your LCA analysis today?</p>
          </div>
        </div>
        
        <div className="flex items-start gap-2">
          <div className="bg-muted rounded-lg px-4 py-2">
            {isTyping ? (
              <TypingIndicator size="sm" />
            ) : (
              <p className="text-sm">I'm analyzing your carbon footprint data and will provide recommendations shortly.</p>
            )}
          </div>
        </div>
        
        <div className="flex items-start gap-2 justify-end">
          <div className="bg-primary text-primary-foreground rounded-lg px-4 py-2">
            <p className="text-sm">That sounds great! What are the main environmental impacts I should focus on?</p>
          </div>
        </div>
        
        <div className="flex items-start gap-2">
          <div className="bg-muted rounded-lg px-4 py-2">
            {isTyping ? (
              <TypingIndicator size="sm" />
            ) : (
              <p className="text-sm">Based on your data, the main impacts are: 1) Manufacturing emissions (65%), 2) Transportation (20%), 3) End-of-life disposal (15%).</p>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

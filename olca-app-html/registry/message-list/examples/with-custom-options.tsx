"use client"

import { MessageList } from "@/components/ui/message-list"

/**
 * MessageList with custom message options example.
 * 
 * This example shows how to use the MessageList component with
 * custom styling and options for all messages.
 */
export function MessageListWithCustomOptions() {
  const messages = [
    {
      id: "1",
      role: "user" as const,
      content: "What are the main environmental impacts of plastic production?",
      createdAt: new Date("2024-01-15T10:30:00"),
    },
    {
      id: "2",
      role: "assistant" as const,
      content: "The main environmental impacts of plastic production include:\n\n1. **Greenhouse Gas Emissions**: Significant CO2 emissions during manufacturing\n2. **Water Pollution**: Chemical runoff and microplastics\n3. **Resource Depletion**: Heavy reliance on fossil fuels\n4. **Waste Generation**: Long-term disposal challenges\n\nWould you like me to elaborate on any of these impacts?",
      createdAt: new Date("2024-01-15T10:30:10"),
    },
  ]

  const handleCopy = (content: string) => {
    navigator.clipboard.writeText(content)
    console.log("Copied to clipboard:", content)
  }

  const handleRateResponse = (messageId: string, rating: "thumbs-up" | "thumbs-down") => {
    console.log(`Rated message ${messageId} with ${rating}`)
  }

  return (
    <div className="w-full max-w-2xl mx-auto p-4">
      <h3 className="text-lg font-semibold mb-4">Message List with Custom Options</h3>
      <p className="text-sm text-muted-foreground mb-4">
        This example shows custom styling, avatars, and action buttons for all messages.
      </p>
      
      <div className="h-[400px] overflow-auto border rounded-lg">
        <MessageList
          messages={messages}
          messageOptions={{
            className: "custom-message-class",
            showAvatar: true,
            onCopy: handleCopy,
            onRateResponse: handleRateResponse,
          }}
        />
      </div>
    </div>
  )
}

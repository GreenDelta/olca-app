"use client"

import { MessageList } from "@/components/ui/message-list"

/**
 * MessageList with dynamic message options example.
 * 
 * This example shows how to use the MessageList component with
 * dynamic styling based on message properties like role.
 */
export function MessageListWithDynamicOptions() {
  const messages = [
    {
      id: "1",
      role: "user" as const,
      content: "Can you help me understand carbon footprint calculations?",
      createdAt: new Date("2024-01-15T10:30:00"),
    },
    {
      id: "2",
      role: "assistant" as const,
      content: "Absolutely! Carbon footprint calculations involve several key steps:\n\n1. **Scope Definition**: Define the system boundaries\n2. **Data Collection**: Gather activity data and emission factors\n3. **Calculation**: Apply the formula: Activity Data × Emission Factor\n4. **Reporting**: Present results in CO2 equivalent units\n\nWould you like me to walk through a specific example?",
      createdAt: new Date("2024-01-15T10:30:15"),
    },
    {
      id: "3",
      role: "user" as const,
      content: "Yes, let's do a simple example with electricity consumption.",
      createdAt: new Date("2024-01-15T10:31:00"),
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
      <h3 className="text-lg font-semibold mb-4">Message List with Dynamic Options</h3>
      <p className="text-sm text-muted-foreground mb-4">
        This example shows dynamic styling where user messages have different styling
        than assistant messages, and only assistant messages show rating buttons.
      </p>
      
      <div className="h-[400px] overflow-auto border rounded-lg">
        <MessageList
          messages={messages}
          messageOptions={(message) => ({
            className: message.role === "user" 
              ? "user-message bg-blue-50 border-l-4 border-blue-500" 
              : "assistant-message bg-gray-50 border-l-4 border-gray-500",
            showAvatar: message.role === "assistant",
            onCopy: handleCopy,
            onRateResponse: message.role === "assistant" ? handleRateResponse : undefined,
          })}
        />
      </div>
    </div>
  )
}

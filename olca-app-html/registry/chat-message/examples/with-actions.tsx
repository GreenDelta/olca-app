"use client"

import { ChatMessage } from "@/components/ui/chat-message"
import { Button } from "@/components/ui/button"
import { ThumbsUp, ThumbsDown, Copy } from "lucide-react"

/**
 * ChatMessage with actions example.
 * 
 * This example shows how to use the ChatMessage component with
 * hover actions for assistant messages.
 */
export function ChatMessageWithActions() {
  const handleRate = (rating: "thumbs-up" | "thumbs-down") => {
    console.log(`Rated message with ${rating}`)
  }

  const handleCopy = () => {
    console.log("Copied message content")
  }

  const actions = (
    <div className="flex gap-1">
      <Button
        variant="ghost"
        size="sm"
        onClick={() => handleRate("thumbs-up")}
        className="h-6 w-6 p-0"
      >
        <ThumbsUp className="h-3 w-3" />
      </Button>
      <Button
        variant="ghost"
        size="sm"
        onClick={() => handleRate("thumbs-down")}
        className="h-6 w-6 p-0"
      >
        <ThumbsDown className="h-3 w-3" />
      </Button>
      <Button
        variant="ghost"
        size="sm"
        onClick={handleCopy}
        className="h-6 w-6 p-0"
      >
        <Copy className="h-3 w-3" />
      </Button>
    </div>
  )

  return (
    <div className="w-full max-w-2xl mx-auto p-4">
      <h3 className="text-lg font-semibold mb-4">Chat Message with Actions</h3>
      <p className="text-sm text-muted-foreground mb-4">
        Hover over assistant messages to see action buttons (thumbs up/down, copy).
        Actions are only shown for assistant messages.
      </p>
      
      <div className="space-y-4">
        <ChatMessage
          id="1"
          role="user"
          content="Can you help me understand carbon footprint calculations?"
          createdAt={new Date("2024-01-15T10:30:00")}
        />
        <ChatMessage
          id="2"
          role="assistant"
          content="Absolutely! Carbon footprint calculations involve several key steps:\n\n1. **Scope Definition**: Define the system boundaries\n2. **Data Collection**: Gather activity data and emission factors\n3. **Calculation**: Apply the formula: Activity Data × Emission Factor\n4. **Reporting**: Present results in CO2 equivalent units\n\nWould you like me to walk through a specific example?"
          createdAt={new Date("2024-01-15T10:30:15")}
          actions={actions}
        />
        <ChatMessage
          id="3"
          role="user"
          content="Yes, let's do a simple example with electricity consumption."
          createdAt={new Date("2024-01-15T10:31:00")}
        />
        <ChatMessage
          id="4"
          role="assistant"
          content="Great! Here's a simple electricity consumption example:\n\n**Example Calculation:**\n- Electricity used: 100 kWh\n- Emission factor: 0.5 kg CO2e/kWh\n- Carbon footprint: 100 × 0.5 = 50 kg CO2e\n\n**Key Points:**\n- Always use the correct emission factor for your region\n- Consider the electricity mix (renewable vs fossil fuels)\n- Include transmission and distribution losses\n\nThis gives you the direct emissions from electricity use (Scope 2)."
          createdAt={new Date("2024-01-15T10:31:15")}
          actions={actions}
        />
      </div>
    </div>
  )
}

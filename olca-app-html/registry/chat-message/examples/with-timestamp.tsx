"use client"

import { ChatMessage } from "@/components/ui/chat-message"

/**
 * ChatMessage with timestamp example.
 * 
 * This example shows how to use the ChatMessage component with
 * timestamp display enabled.
 */
export function ChatMessageWithTimestamp() {
  return (
    <div className="w-full max-w-2xl mx-auto p-4">
      <h3 className="text-lg font-semibold mb-4">Chat Message with Timestamp</h3>
      <p className="text-sm text-muted-foreground mb-4">
        Messages with timestamps displayed below each message bubble.
      </p>
      
      <div className="space-y-4">
        <ChatMessage
          id="1"
          role="user"
          content="What are the main environmental impacts of plastic production?"
          createdAt={new Date("2024-01-15T10:30:00")}
          showTimeStamp
        />
        <ChatMessage
          id="2"
          role="assistant"
          content="The main environmental impacts of plastic production include:\n\n🌱 **Greenhouse Gas Emissions**: Significant CO2 emissions during manufacturing\n💰 **Water Pollution**: Chemical runoff and microplastics\n🔋 **Resource Depletion**: Heavy reliance on fossil fuels\n♻️ **Waste Generation**: Long-term disposal challenges\n\nWould you like me to elaborate on any of these impacts?"
          createdAt={new Date("2024-01-15T10:30:10")}
          showTimeStamp
        />
        <ChatMessage
          id="3"
          role="user"
          content="Can you focus on the water pollution aspect?"
          createdAt={new Date("2024-01-15T10:31:00")}
          showTimeStamp
        />
        <ChatMessage
          id="4"
          role="assistant"
          content="Certainly! Water pollution from plastic production is a significant concern:\n\n**Direct Impacts:**\n- Chemical runoff from manufacturing facilities\n- Microplastics entering waterways\n- Contamination of drinking water sources\n\n**Indirect Impacts:**\n- Plastic waste breaking down in oceans\n- Bioaccumulation in marine life\n- Disruption of aquatic ecosystems\n\n**Mitigation Strategies:**\n- Improved wastewater treatment\n- Biodegradable alternatives\n- Circular economy approaches"
          createdAt={new Date("2024-01-15T10:31:15")}
          showTimeStamp
        />
      </div>
    </div>
  )
}

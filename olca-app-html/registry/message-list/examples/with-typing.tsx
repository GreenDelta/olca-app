"use client"

import { useState, useEffect } from "react"
import { MessageList } from "@/components/ui/message-list"

/**
 * MessageList with typing indicator example.
 * 
 * This example shows how to use the MessageList component with
 * a typing indicator to show when the AI is generating a response.
 */
export function MessageListWithTyping() {
  const [messages, setMessages] = useState([
    {
      id: "1",
      role: "user" as const,
      content: "What are the benefits of renewable energy?",
      createdAt: new Date("2024-01-15T10:30:00"),
    },
  ])
  const [isTyping, setIsTyping] = useState(false)

  useEffect(() => {
    // Simulate AI response after 3 seconds
    const timer = setTimeout(() => {
      setIsTyping(false)
      setMessages(prev => [
        ...prev,
        {
          id: "2",
          role: "assistant" as const,
          content: "Renewable energy offers numerous benefits:\n\n🌱 **Environmental**: Reduces greenhouse gas emissions and air pollution\n💰 **Economic**: Creates jobs and reduces energy costs over time\n🔋 **Energy Security**: Reduces dependence on fossil fuel imports\n♻️ **Sustainability**: Uses inexhaustible natural resources\n\nThese benefits make renewable energy a key solution for sustainable development.",
          createdAt: new Date("2024-01-15T10:30:10"),
        }
      ])
    }, 3000)

    // Start typing indicator after 1 second
    const typingTimer = setTimeout(() => {
      setIsTyping(true)
    }, 1000)

    return () => {
      clearTimeout(timer)
      clearTimeout(typingTimer)
    }
  }, [])

  const handleCopy = (content: string) => {
    navigator.clipboard.writeText(content)
    console.log("Copied to clipboard:", content)
  }

  return (
    <div className="w-full max-w-2xl mx-auto p-4">
      <h3 className="text-lg font-semibold mb-4">Message List with Typing Indicator</h3>
      <p className="text-sm text-muted-foreground mb-4">
        This example shows a typing indicator that appears when the AI is generating a response.
        The indicator will disappear when the response is complete.
      </p>
      
      <div className="h-[400px] overflow-auto border rounded-lg">
        <MessageList
          messages={messages}
          isTyping={isTyping}
          messageOptions={{
            showAvatar: true,
            onCopy: handleCopy,
          }}
        />
      </div>
    </div>
  )
}

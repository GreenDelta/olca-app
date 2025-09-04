"use client"

import * as React from "react"
import { cn } from "@/lib/utils"
import { ChatContainer } from "./chat-container"
import { ChatForm } from "./chat-form"
import { ChatMessages } from "./chat-messages"
import { MessageInput } from "./message-input"
import { MessageList } from "./message-list"
import { PromptSuggestions } from "./prompt-suggestions"

export interface Message {
  id: string
  role: "user" | "assistant" | "system"
  content: string
  createdAt?: Date
}

/**
 * A composable and customizable chat interface component.
 * 
 * Provides a complete chat interface with message history, typing indicators, 
 * file attachments support, and auto-scrolling behavior.
 * 
 * @example
 * ```tsx
 * "use client"
 * import { useChat } from "ai/react"
 * import { Chat } from "@/components/ui/chat"
 * 
 * export function ChatDemo() {
 *   const { messages, input, handleInputChange, handleSubmit, status, stop } = useChat()
 *   const isLoading = status === "submitted" || status === "streaming"
 * 
 *   return (
 *     <Chat
 *       messages={messages}
 *       input={input}
 *       handleInputChange={handleInputChange}
 *       handleSubmit={handleSubmit}
 *       isGenerating={isLoading}
 *       stop={stop}
 *     />
 *   )
 * }
 * ```
 * 
 * @example
 * ```tsx
 * // With prompt suggestions
 * export function ChatWithSuggestions() {
 *   const { messages, input, handleInputChange, handleSubmit, append, status, stop } = useChat()
 *   const isLoading = status === "submitted" || status === "streaming"
 * 
 *   return (
 *     <Chat
 *       messages={messages}
 *       input={input}
 *       handleInputChange={handleInputChange}
 *       handleSubmit={handleSubmit}
 *       isGenerating={isLoading}
 *       stop={stop}
 *       append={append}
 *       suggestions={[
 *         "Generate a tasty vegan lasagna recipe for 3 people.",
 *         "Generate a list of 5 questions for a frontend job interview.",
 *         "Who won the 2022 FIFA World Cup?",
 *       ]}
 *     />
 *   )
 * }
 * ```
 * 
 * @param messages - Array of chat messages to display
 * @param input - Current input value
 * @param handleInputChange - Input change handler
 * @param handleSubmit - Form submission handler
 * @param isGenerating - Whether AI is currently generating a response
 * @param stop - Function to stop AI generation
 * @param setMessages - Optional function to update messages state. When provided, enables enhanced tool cancellation handling
 * @param append - Function to append a new message (required for suggestions)
 * @param suggestions - Array of prompt suggestions to show when chat is empty
 * @param onRateResponse - Callback to handle user rating of AI responses, if not provided the rating buttons will not be displayed
 * @param className - Additional CSS classes for ChatContainer
 * @param transcribeAudio - Function to transcribe audio (required for voice input)
 */
export interface ChatProps {
  messages: Message[]
  input: string
  handleInputChange: (e: React.ChangeEvent<HTMLTextAreaElement>) => void
  handleSubmit: (event?: React.FormEvent, options?: any) => void
  isGenerating: boolean
  stop: () => void
  setMessages?: (messages: Message[]) => void
  append?: (message: Message) => void
  suggestions?: string[]
  onRateResponse?: (messageId: string, rating: "thumbs-up" | "thumbs-down") => void
  className?: string
  transcribeAudio?: (blob: Blob) => Promise<string>
}

export function Chat({
  messages,
  input,
  handleInputChange,
  handleSubmit,
  isGenerating,
  stop,
  setMessages,
  append,
  suggestions,
  onRateResponse,
  className,
  transcribeAudio,
}: ChatProps) {
  const lastMessage = messages.at(-1)
  const isEmpty = messages.length === 0
  const isTyping = lastMessage?.role === "user"

  return (
    <ChatContainer className={className}>
      {isEmpty && suggestions && append ? (
        <PromptSuggestions
          append={append}
          suggestions={suggestions}
        />
      ) : null}

      {!isEmpty ? (
        <ChatMessages>
          <MessageList 
            messages={messages} 
            isTyping={isTyping}
            onRateResponse={onRateResponse}
          />
        </ChatMessages>
      ) : null}

      <ChatForm
        className="mt-auto"
        isPending={isGenerating || isTyping}
        handleSubmit={handleSubmit}
      >
        {({ files, setFiles }) => (
          <MessageInput
            value={input}
            onChange={handleInputChange}
            allowAttachments
            files={files}
            setFiles={setFiles}
            stop={stop}
            isGenerating={isGenerating}
            transcribeAudio={transcribeAudio}
          />
        )}
      </ChatForm>
    </ChatContainer>
  )
}

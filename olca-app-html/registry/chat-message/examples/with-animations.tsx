"use client"

import { ChatMessage } from "@/components/ui/chat-message"

/**
 * ChatMessage with different animation styles example.
 * 
 * This example shows how to use the ChatMessage component with
 * different animation styles for message appearance.
 */
export function ChatMessageWithAnimations() {
  return (
    <div className="w-full max-w-2xl mx-auto p-4">
      <h3 className="text-lg font-semibold mb-4">Chat Message with Animations</h3>
      <p className="text-sm text-muted-foreground mb-4">
        Different animation styles for message appearance: slide, scale, fade, and none.
      </p>
      
      <div className="space-y-6">
        <div>
          <h4 className="text-sm font-medium mb-2">Slide Animation</h4>
          <div className="space-y-4">
            <ChatMessage
              id="1"
              role="user"
              content="Slide animation from the right"
              animation="slide"
            />
            <ChatMessage
              id="2"
              role="assistant"
              content="Slide animation from the left"
              animation="slide"
            />
          </div>
        </div>

        <div>
          <h4 className="text-sm font-medium mb-2">Scale Animation (Default)</h4>
          <div className="space-y-4">
            <ChatMessage
              id="3"
              role="user"
              content="Scale animation - grows from center"
              animation="scale"
            />
            <ChatMessage
              id="4"
              role="assistant"
              content="Scale animation - grows from center"
              animation="scale"
            />
          </div>
        </div>

        <div>
          <h4 className="text-sm font-medium mb-2">Fade Animation</h4>
          <div className="space-y-4">
            <ChatMessage
              id="5"
              role="user"
              content="Fade animation - appears smoothly"
              animation="fade"
            />
            <ChatMessage
              id="6"
              role="assistant"
              content="Fade animation - appears smoothly"
              animation="fade"
            />
          </div>
        </div>

        <div>
          <h4 className="text-sm font-medium mb-2">No Animation</h4>
          <div className="space-y-4">
            <ChatMessage
              id="7"
              role="user"
              content="No animation - appears instantly"
              animation="none"
            />
            <ChatMessage
              id="8"
              role="assistant"
              content="No animation - appears instantly"
              animation="none"
            />
          </div>
        </div>
      </div>
    </div>
  )
}

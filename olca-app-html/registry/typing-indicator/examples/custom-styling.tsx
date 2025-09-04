"use client"

import { TypingIndicator } from "@/components/ui/typing-indicator"

/**
 * TypingIndicator with custom styling example.
 * 
 * This example shows how to use the TypingIndicator component with
 * different sizes and color variants.
 */
export function CustomStylingTypingIndicator() {
  return (
    <div className="w-full max-w-2xl mx-auto p-4">
      <h3 className="text-lg font-semibold mb-4">Custom Styling Typing Indicator</h3>
      <p className="text-sm text-muted-foreground mb-4">
        Different sizes and color variants of the typing indicator.
      </p>
      
      <div className="space-y-6">
        <div>
          <h4 className="text-sm font-medium mb-2">Sizes</h4>
          <div className="space-y-4">
            <div className="flex items-center gap-4">
              <span className="text-sm w-8">Small:</span>
              <div className="flex items-center p-4 bg-muted rounded-lg">
                <TypingIndicator size="sm" />
              </div>
            </div>
            <div className="flex items-center gap-4">
              <span className="text-sm w-8">Medium:</span>
              <div className="flex items-center p-4 bg-muted rounded-lg">
                <TypingIndicator size="md" />
              </div>
            </div>
            <div className="flex items-center gap-4">
              <span className="text-sm w-8">Large:</span>
              <div className="flex items-center p-4 bg-muted rounded-lg">
                <TypingIndicator size="lg" />
              </div>
            </div>
          </div>
        </div>

        <div>
          <h4 className="text-sm font-medium mb-2">Colors</h4>
          <div className="space-y-4">
            <div className="flex items-center gap-4">
              <span className="text-sm w-16">Default:</span>
              <div className="flex items-center p-4 bg-muted rounded-lg">
                <TypingIndicator color="default" />
              </div>
            </div>
            <div className="flex items-center gap-4">
              <span className="text-sm w-16">Primary:</span>
              <div className="flex items-center p-4 bg-muted rounded-lg">
                <TypingIndicator color="primary" />
              </div>
            </div>
            <div className="flex items-center gap-4">
              <span className="text-sm w-16">Muted:</span>
              <div className="flex items-center p-4 bg-muted rounded-lg">
                <TypingIndicator color="muted" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

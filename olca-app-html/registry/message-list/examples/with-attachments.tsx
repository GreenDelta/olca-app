"use client"

import { MessageList } from "@/components/ui/message-list"

/**
 * MessageList with file attachments example.
 * 
 * This example shows how to use the MessageList component with
 * messages that include file attachments.
 */
export function MessageListWithAttachments() {
  // Create mock file objects
  const mockFile1 = new File(["mock content"], "lca-report.pdf", { type: "application/pdf" })
  const mockFile2 = new File(["mock content"], "emission-data.xlsx", { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" })
  const mockFile3 = new File(["mock content"], "sustainability-guidelines.docx", { type: "application/vnd.openxmlformats-officedocument.wordprocessingml.document" })

  const messages = [
    {
      id: "1",
      role: "user" as const,
      content: "I've attached some documents for the LCA analysis. Can you help me review them?",
      createdAt: new Date("2024-01-15T10:30:00"),
      attachments: [mockFile1, mockFile2],
    },
    {
      id: "2",
      role: "assistant" as const,
      content: "I can see you've shared your LCA report and emission data. Here's my analysis:\n\n📊 **LCA Report**: Well-structured with clear system boundaries\n📈 **Emission Data**: Comprehensive coverage of Scope 1, 2, and 3 emissions\n\n**Key Findings**:\n- Total carbon footprint: 2.3 tCO2e per unit\n- Manufacturing phase contributes 65% of total emissions\n- Transportation shows significant impact (20%)\n\n**Recommendations**:\n1. Focus on renewable energy for manufacturing\n2. Optimize supply chain logistics\n3. Consider circular economy principles\n\nWould you like me to elaborate on any specific aspect?",
      createdAt: new Date("2024-01-15T10:30:15"),
    },
    {
      id: "3",
      role: "user" as const,
      content: "Thanks! I also have our sustainability guidelines document.",
      createdAt: new Date("2024-01-15T10:31:00"),
      attachments: [mockFile3],
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
      <h3 className="text-lg font-semibold mb-4">Message List with Attachments</h3>
      <p className="text-sm text-muted-foreground mb-4">
        This example shows messages with file attachments. Attachments are displayed
        as small badges with file icons and names.
      </p>
      
      <div className="h-[500px] overflow-auto border rounded-lg">
        <MessageList
          messages={messages}
          messageOptions={{
            showAvatar: true,
            onCopy: handleCopy,
            onRateResponse: handleRateResponse,
          }}
        />
      </div>
    </div>
  )
}

"use client"

import { ChatMessage } from "@/components/ui/chat-message"

/**
 * ChatMessage with file attachments example.
 * 
 * This example shows how to use the ChatMessage component with
 * file attachments displayed as badges.
 */
export function ChatMessageWithAttachments() {
  // Create mock file objects
  const mockFile1 = new File(["mock content"], "lca-report.pdf", { type: "application/pdf" })
  const mockFile2 = new File(["mock content"], "emission-data.xlsx", { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" })
  const mockFile3 = new File(["mock content"], "sustainability-guidelines.docx", { type: "application/vnd.openxmlformats-officedocument.wordprocessingml.document" })

  return (
    <div className="w-full max-w-2xl mx-auto p-4">
      <h3 className="text-lg font-semibold mb-4">Chat Message with Attachments</h3>
      <p className="text-sm text-muted-foreground mb-4">
        Messages with file attachments are displayed as small badges with file icons and names.
      </p>
      
      <div className="space-y-4">
        <ChatMessage
          id="1"
          role="user"
          content="I've attached some documents for the LCA analysis. Can you help me review them?"
          createdAt={new Date("2024-01-15T10:30:00")}
          attachments={[mockFile1, mockFile2]}
        />
        <ChatMessage
          id="2"
          role="assistant"
          content="I can see you've shared your LCA report and emission data. Here's my analysis:\n\n📊 **LCA Report**: Well-structured with clear system boundaries\n📈 **Emission Data**: Comprehensive coverage of Scope 1, 2, and 3 emissions\n\n**Key Findings:**\n- Total carbon footprint: 2.3 tCO2e per unit\n- Manufacturing phase contributes 65% of total emissions\n- Transportation shows significant impact (20%)\n\n**Recommendations:**\n1. Focus on renewable energy for manufacturing\n2. Optimize supply chain logistics\n3. Consider circular economy principles\n\nWould you like me to elaborate on any specific aspect?"
          createdAt={new Date("2024-01-15T10:30:15")}
        />
        <ChatMessage
          id="3"
          role="user"
          content="Thanks! I also have our sustainability guidelines document."
          createdAt={new Date("2024-01-15T10:31:00")}
          attachments={[mockFile3]}
        />
        <ChatMessage
          id="4"
          role="assistant"
          content="Perfect! I can see your sustainability guidelines document. This will help provide context for the recommendations.\n\n**Integration with Guidelines:**\n- Aligns with your circular economy objectives\n- Supports your renewable energy targets\n- Complements your supply chain optimization goals\n\n**Next Steps:**\n1. Review the guidelines against current practices\n2. Identify gaps in implementation\n3. Develop action plans for improvement\n\nWould you like me to help create a roadmap based on these documents?"
          createdAt={new Date("2024-01-15T10:31:15")}
        />
      </div>
    </div>
  )
}

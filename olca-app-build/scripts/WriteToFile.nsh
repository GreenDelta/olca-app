# a macro for using the WriteToFile function (see below)
# NewLine indicates if a carriage return should be added at the end of the text
!macro WriteToFile NewLine File Text
  
  # push the text on the stack
  !if `${NewLine}` == true
    Push `${Text}$\r$\n`
  !else
    Push `${Text}`
  !endif
  
  # push the file on the stack
  Push `${File}`
  
  # call the function
  Call WriteToFile
!macroend
!define WriteToFile `!insertmacro WriteToFile false`
!define WriteLineToFile `!insertmacro WriteToFile true`

# function for writing text to a file
# the stack should look like [file, text] when calling this function
Function WriteToFile
  Exch $0 # put the file to register $0
  Exch	# change the top two stack values
  Exch $1 # put the text to register $1
 
  FileOpen $0 $0 a 	# open the file
  FileSeek $0 0 END # go to end
  FileWrite $0 $1 	# write to file
  FileClose $0		# close the file
 
Pop $1	# remove the text and the file from the stack
Pop $0
FunctionEnd
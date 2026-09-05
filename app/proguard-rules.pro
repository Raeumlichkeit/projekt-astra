# Project-specific R8 rules. AndroidX, Compose, CameraX and Astronomy Engine publish
# their consumer rules; keep source/line information for useful Play Console traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Optional PDFBox JPX image support is not used when extracting text from IMO calendars.
-dontwarn com.gemalto.jp2.JP2Decoder
# Public-key PDF decryption is also excluded; IMO publishes unencrypted calendars.
-dontwarn org.bouncycastle.**

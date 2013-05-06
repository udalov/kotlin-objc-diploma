#import "char.h"

@implementation A
+ (void) printTwoChars: (char)firstChar with:(char)secondChar {
    printf("%c%c", firstChar, secondChar);
}
@end

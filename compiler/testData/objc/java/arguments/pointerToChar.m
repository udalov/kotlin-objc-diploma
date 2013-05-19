#import "pointerToChar.h"

#import <stdio.h>

@implementation A
+ (void) printTwoChars: (char *)first secondChar: (char *)second {
    if (!first || !second)
        printf("NULL pointers passed: %p %p\n", first, second);
    else
        printf("%c%c", *first, *second);
}
@end

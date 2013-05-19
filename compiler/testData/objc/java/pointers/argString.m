#import "argString.h"

#import <stdio.h>

@implementation A
+ (void) printString: (const char *) string {
    printf("%s", string);
}
@end

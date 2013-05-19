#import "argNSString.h"

#import <stdio.h>

@implementation A
+ (void) printString: (NSString *) string {
    printf("%s", [string cStringUsingEncoding: NSASCIIStringEncoding]);
}
@end

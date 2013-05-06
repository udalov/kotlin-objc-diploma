#import "short.h"

@implementation A
+ (void) printOKIf42: (short) arg {
    if (arg == 42)
        printf("OK");
    else
        printf("Fail %d", arg);
}
@end

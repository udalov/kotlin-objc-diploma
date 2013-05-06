#import "int.h"
#import <stdio.h>

@implementation A
+ (void) printOKIf42: (int) arg {
    if (arg == 42)
        printf("OK");
    else
        printf("Fail %d", arg);
}
@end

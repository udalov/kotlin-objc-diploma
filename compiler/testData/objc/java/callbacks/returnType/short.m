#import "short.h"

#import <stdio.h>

@implementation A
+ (void) printOKIf42: (fun_t) fun {
    short result = fun();
    if (result == 42)
        printf("OK");
    else
        printf("Fail: %d", result);
}
@end

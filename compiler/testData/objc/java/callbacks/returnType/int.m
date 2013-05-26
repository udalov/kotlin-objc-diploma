#import "int.h"

#import <stdio.h>

@implementation A
+ (void) printOKIf42: (fun_t) fun {
    int result = fun();
    if (result == 42)
        printf("OK");
    else
        printf("Fail: %d", result);
}
@end

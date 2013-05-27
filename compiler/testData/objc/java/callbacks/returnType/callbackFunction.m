#import "callbackFunction.h"

#import <stdio.h>

@implementation A
+ (void) printOKIf42: (megafun_t) megafun {
    fun_t fun = megafun();
    int result = fun();
    if (result == 42)
        printf("OK");
    else
        printf("Fail: %d", result);
}
@end

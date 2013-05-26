#import "long.h"

#import <stdio.h>

@implementation A
+ (void) checkResultAndPrintOK: (fun_t) fun {
    long long result = fun();
    if (result == 12345678987654321LL)
        printf("OK");
    else
        printf("Fail: %lld", result);
}
@end

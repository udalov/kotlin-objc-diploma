#import "float.h"

#import <math.h>
#import <stdio.h>

@implementation A
+ (void) printOKIfPi: (fun_t) fun {
    float result = fun();
    if (fabsf(result - 3.14159265) < 1e-5)
        printf("OK");
    else
        printf("Fail: %.20f", result);
}
@end

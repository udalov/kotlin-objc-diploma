#import "double.h"

#import <math.h>
#import <stdio.h>

@implementation A
+ (void) printOKIfPi: (fun_t) fun {
    double result = fun();
    if (fabs(result - 3.14159265) < 1e-8)
        printf("OK");
    else
        printf("Fail: %.20lf", result);
}
@end

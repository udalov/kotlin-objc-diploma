#import "double.h"
#import <stdio.h>
#import <math.h>

@implementation A
+ (void) printOKIfPi: (double) arg {
    if (fabs(arg - 3.14159265) < 1e-8)
        printf("OK");
    else
        printf("Fail %.20lf", arg);
}
@end


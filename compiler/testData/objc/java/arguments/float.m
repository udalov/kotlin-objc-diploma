#import "float.h"
#import <stdio.h>
#import <math.h>

@implementation A
+ (void) printOKIfPi: (float) arg {
    if (fabsf(arg - 3.14159265) < 1e-5)
        printf("OK");
    else
        printf("Fail %.20f", arg);
}
@end


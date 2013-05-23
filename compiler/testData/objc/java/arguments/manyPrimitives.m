#import "manyPrimitives.h"

#import "stdio.h"
#import "math.h"

@implementation A
+ (void) printOKIfPiAndFortyTwoAndCapitalZAndMinusOne:
    (float) pi : (short) fortyTwo : (char) capitalZ : (int) minusOne {

    if (fabsf(pi - 3.14) > 0.00001) printf("Fail pi: %.20f", pi);
    else if (fortyTwo != 42) printf("Fail 42: %d", fortyTwo);
    else if (capitalZ != 'Z') printf("Fail Z: %c", capitalZ);
    else if (minusOne != -1) printf("Fail -1: %d", minusOne);
    else printf("OK");
}
@end

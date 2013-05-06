#import "bool.h"
#import "stdio.h"

@implementation A
+ (void) printOIfTrue: (BOOL) arg {
    if (arg) printf("O");
}

+ (void) printKIfFalse: (BOOL) arg {
    if (!arg) printf("K");
}
@end

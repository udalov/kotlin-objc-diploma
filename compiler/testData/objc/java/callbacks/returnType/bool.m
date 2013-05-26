#import "bool.h"

#import <stdio.h>

@implementation A
+ (void) printOIfTrue: (fun_t) fun {
    BOOL result = fun();
    if (result)
        printf("O");
    else
        printf("{Fail true}");
}
+ (void) printKIfFalse: (fun_t) fun {
    BOOL result = fun();
    if (result)
        printf("{Fail false}");
    else
        printf("K");
}
@end

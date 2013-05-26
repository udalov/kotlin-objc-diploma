#import "char.h"

#import <stdio.h>

@implementation A
+ (void) printOKIfLowercaseA: (fun_t) fun {
    char result = fun();
    if (result == 'a')
        printf("OK");
    else
        printf("Fail: %c", result);
}
@end

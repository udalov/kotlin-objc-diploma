#import "callCategoryMethod.h"

#import "stdio.h"

@implementation A
+ (A *) create {
    return [[A alloc] init];
}
@end

@implementation A (Category)
- (void) printOK {
    printf("OK");
}
@end

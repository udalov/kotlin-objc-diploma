#import "callCategoryMetaclassMethod.h"

#import "stdio.h"

@implementation A
@end

@implementation A (Category)
+ (void) printOK {
    printf("OK");
}
@end

#import <Foundation/NSObject.h>

@interface A : NSObject
+ (A *) create;
@end

@interface A (Category)
- (void) printOK;
@end

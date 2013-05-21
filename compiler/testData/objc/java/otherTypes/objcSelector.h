#import <Foundation/NSObject.h>

@interface A : NSObject
+ (SEL) getSelector;

+ (void) invokeSelector: (SEL) selector;

+ (void) printOK;
@end

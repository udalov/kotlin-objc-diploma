#import <Foundation/NSObject.h>

typedef void (*fun_t)(SEL);

@interface A : NSObject
+ (void) invoke: (fun_t) callback;

+ (void) checkIfEqualsToInvoke: (SEL) selector;
@end

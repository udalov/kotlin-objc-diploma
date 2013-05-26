#import <Foundation/NSObject.h>

typedef short (*fun_t)();

@interface A : NSObject
+ (void) printOKIf42: (fun_t) fun;
@end
